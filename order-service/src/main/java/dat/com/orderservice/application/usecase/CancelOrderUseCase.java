package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.saga.OrderSagaState;
import dat.com.orderservice.application.saga.OrderSagaType;
import dat.com.orderservice.config.KafkaTopicConfig;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.model.OrderStatus;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.application.command.ReleaseStockCommand;
import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import dat.com.orderservice.infrastructure.persistence.entity.OrderSagaEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderOutboxJpaRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderSagaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

//    private static final String RELEASE_REQUESTED =
//            "inventory.release.requested";
//
//    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
//    private final ObjectMapper objectMapper;
//    private final OrderRepository orderRepository;
//
//    @Transactional
//    public Order execute(UUID orderId) {
//
//        //1đọc order
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));
//
//        //2.order quết  định có cần chuyển trạng thai k
//        boolean changed = order.requestCancellation();
//
//        //đã xaác định cancelling hoặc canceled thì k tạo thêm command
//        if(!changed){
//            return order;
//        }
//
//        //3.lưu cancelling, ktra version
//        Order saved = orderRepository.save(order);
//
//        //4Tạo event yêu cầu giải phóng kho
//
//        UUID eventId = UUID.randomUUID();
//        ReleaseStockCommand command = new ReleaseStockCommand(eventId,saved.getId(), saved.getProductId(), saved.getQuantity());
//
//        //5.Lưu command vào outbox
//        orderOutboxJpaRepository.save(OrderOutboxEntity.create(eventId,saved.getId(),RELEASE_REQUESTED,objectMapper.writeValueAsString(command)));
//
//
//        return saved;
//    }

    private final OrderRepository orderRepository;
    private final OrderSagaJpaRepository sagaRepository;
    private final OrderOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${order.saga.release-timeout:PT2M}")
    private Duration releaseTimeout;

    @Transactional
    public Order execute(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        // Hủy lặp không tạo thêm Saga hoặc command.
        if (order.getStatus() == OrderStatus.CANCELLING
                || order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        // Bước chuyển đổi này chỉ xử lý đơn có Saga mới.
        OrderSagaEntity createSaga = sagaRepository
                .findByOrderIdAndType(
                        orderId,
                        OrderSagaType.CREATE_ORDER
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đơn chưa có Saga Orchestration"
                ));

        OrderSagaEntity releaseSaga;

        if (order.getStatus() == OrderStatus.PENDING) {

            createSaga.requestRelease(releaseTimeout);
            releaseSaga = createSaga;

        } else if (order.getStatus() == OrderStatus.CONFIRM) {

            if (createSaga.getState() != OrderSagaState.COMPLETED) {
                throw new IllegalStateException(
                        "Order CONFIRM nhưng Saga tạo đơn chưa COMPLETED"
                );
            }

            releaseSaga = OrderSagaEntity.startCancelOrder(
                    orderId,
                    releaseTimeout
            );

        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể hủy đơn ở trạng thái: "
                            + order.getStatus()
            );
        }

        // Ghi nhận ý định hủy trong domain Order.
        order.requestCancellation();

        Order savedOrder = orderRepository.save(order);

        sagaRepository.save(releaseSaga);

        ReleaseStockCommand command = new ReleaseStockCommand(
                releaseSaga.getCurrentCommandId(),
                releaseSaga.getId(),
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity()
        );

        String payload = objectMapper.writeValueAsString(command);

        OrderOutboxEntity outbox = OrderOutboxEntity.create(
                command.commandId(),
                savedOrder.getId(),
                KafkaTopicConfig.INVENTORY_RELEASE_COMMAND,
                payload
        );

        outboxRepository.save(outbox);

        return savedOrder;
    }
}
