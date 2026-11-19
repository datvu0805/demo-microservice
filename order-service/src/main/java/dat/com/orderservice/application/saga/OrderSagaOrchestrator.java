package dat.com.orderservice.application.saga;

import dat.com.orderservice.application.event.ReserveStockOutcome;
import dat.com.orderservice.application.event.ReserveStockResult;
import dat.com.orderservice.application.event.StockReleasedEvent;
import dat.com.orderservice.config.KafkaTopicConfig;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.model.OrderStatus;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderSagaEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderInboxJpaRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderSagaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderSagaJpaRepository sagaRepository;
    private final OrderRepository orderRepository;
    private final OrderInboxJpaRepository inboxRepository;

    @Transactional
    public void onReserveResult(ReserveStockResult result) {
        Objects.requireNonNull(result, "result không được null");
        result.validate();

        // 1. Chống xử lý lại cùng eventId.
        int inserted = inboxRepository.claimEvent(
                result.eventId().toString(),
                result.orderId().toString(),
                KafkaTopicConfig.INVENTORY_RESERVE_RESULT
        );

        if (inserted == 0) {
            log.info(
                    "Skip duplicate reserve result: eventId={}",
                    result.eventId()
            );
            return;
        }

        // 2. Tìm đúng Saga.
        OrderSagaEntity saga = sagaRepository.findById(result.sagaId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy Saga: " + result.sagaId()
                ));

        // 3. Kết quả phải thuộc đúng đơn và command.
        verifyCorrelation(saga, result);

        // 4. Đọc Order và đối chiếu nội dung.
        Order order = orderRepository.findById(result.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy Order: " + result.orderId()
                ));

        if (!Objects.equals(
                order.getProductId(),
                result.productId()
        ) || !Objects.equals(
                order.getQuantity(),
                result.quantity()
        )) {
            throw new IllegalArgumentException(
                    "Kết quả reserve không khớp nội dung Order"
            );
        }


        if (saga.getState() == OrderSagaState.WAITING_RELEASE
                || saga.getState() == OrderSagaState.COMPENSATED) {

            if (order.getStatus() != OrderStatus.CANCELLING
                    && order.getStatus() != OrderStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Saga đang bù trừ nhưng Order không ở trạng thái hủy"
                );
            }

            log.info(
                    "Ignore late reserve result during compensation: "
                            + "sagaId={}, commandId={}, outcome={}",
                    saga.getId(),
                    result.commandId(),
                    result.outcome()
            );

            return;
        }

        // 5. Phòng trường hợp cùng kết quả nhưng eventId khác.
        if (isAlreadyCompletedWithSameResult(saga, result)) {
            log.info(
                    "Saga already handled reserve result: sagaId={}, state={}",
                    saga.getId(),
                    saga.getState()
            );
            return;
        }

        // 6. Bước này chỉ xử lý luồng đang chờ giữ hàng.
        if (saga.getState() != OrderSagaState.WAITING_STOCK) {
            throw new IllegalStateException(
                    "Không thể xử lý reserve result ở Saga state: "
                            + saga.getState()
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Saga chờ stock nhưng Order không PENDING: "
                            + order.getStatus()
            );
        }

        // 7. Orchestrator quyết định kết quả của quy trình.
        switch (result.outcome()) {
            case RESERVED -> {
                order.confirm();
                saga.completeStockReservation();
            }

            case REJECTED -> {
                order.rejectStock();
                saga.rejectStockReservation();
            }

            case ALREADY_RELEASED -> throw new IllegalStateException(
                    "Inventory đã RELEASED khi Saga vẫn WAITING_STOCK; "
                            + "cần đối soát luồng hủy"
            );
        }

        // 8. Order và Saga được lưu trong cùng transaction với Inbox.
        orderRepository.save(order);

        sagaRepository.saveAndFlush(saga);

        log.info(
                "Applied reserve result: sagaId={}, orderId={}, "
                        + "outcome={}, sagaState={}, orderStatus={}",
                saga.getId(),
                order.getId(),
                result.outcome(),
                saga.getState(),
                order.getStatus()
        );

    }

    @Transactional
    public void onStockReleased(StockReleasedEvent event) {
        Objects.requireNonNull(event, "event không được null");
        if (event.eventId() == null
                || event.orderId() == null
                || event.productId() == null
                || event.productId() <= 0
                || event.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "StockReleasedEvent không hợp lệ"
            );
        }

        // Dedupe theo kết quả đã gắn trực tiếp với saga. Dùng marker riêng
        // để không tranh quyền claim inbox với consumer choreography.
        if (sagaRepository.findByReleaseEventId(event.eventId()).isPresent()) {
            log.info("Skip duplicate orchestrated release: eventId={}", event.eventId());
            return;
        }

        var waitingSagas = sagaRepository.findAllByOrderId(event.orderId())
                .stream()
                .filter(saga -> saga.getState() == OrderSagaState.WAITING_RELEASE)
                .toList();

        if (waitingSagas.isEmpty()) {
            // stock.released cũng được choreography phát cho các order cũ.
            return;
        }
        if (waitingSagas.size() != 1) {
            throw new IllegalStateException(
                    "Có nhiều Saga cùng chờ release cho Order: " + event.orderId()
            );
        }

        OrderSagaEntity saga = waitingSagas.getFirst();
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy Order: " + event.orderId()
                ));

        if (!saga.getOrderId().equals(event.orderId())
                || !Objects.equals(order.getProductId(), event.productId())
                || !Objects.equals(order.getQuantity(), event.quantity())) {
            throw new IllegalArgumentException(
                    "Kết quả release không khớp Saga/Order"
            );
        }

        if (order.getStatus() == OrderStatus.CANCELLING) {
            order.completeCancelaion();
            orderRepository.save(order);
        } else if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Saga chờ release nhưng Order không ở trạng thái hủy: "
                            + order.getStatus()
            );
        }

        saga.completeRelease(event.eventId());
        sagaRepository.saveAndFlush(saga);

        log.info(
                "Completed release compensation: sagaId={}, orderId={}, eventId={}",
                saga.getId(),
                order.getId(),
                event.eventId()
        );
    }

    private void verifyCorrelation(
            OrderSagaEntity saga,
            ReserveStockResult result
    ) {
        if (saga.getType() != OrderSagaType.CREATE_ORDER) {
            throw new IllegalArgumentException(
                    "Reserve result không thuộc CREATE_ORDER Saga"
            );
        }

        if (!saga.getOrderId().equals(result.orderId())) {
            throw new IllegalArgumentException(
                    "orderId không khớp Saga"
            );
        }

        if (!Objects.equals(
                saga.getReserveCommandId(),
                result.commandId()
        )) {
            throw new IllegalArgumentException(
                    "commandId không khớp lệnh reserve của Saga"
            );
        }
    }

    private boolean isAlreadyCompletedWithSameResult(
            OrderSagaEntity saga,
            ReserveStockResult result
    ) {
        return (
                saga.getState() == OrderSagaState.COMPLETED
                        && result.outcome() == ReserveStockOutcome.RESERVED
        ) || (
                saga.getState() == OrderSagaState.REJECTED
                        && result.outcome() == ReserveStockOutcome.REJECTED
        );
    }
}
