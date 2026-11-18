package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.event.StockReleasedEvent;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderInboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcessStockReleasedUseCase {

    private static final String TOPIC = "stock.released";

    private final OrderInboxJpaRepository inboxRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void execute(StockReleasedEvent event) {

        // 1. Kiểm tra dữ liệu sự kiện.
        validate(event);

        // 2. Claim event trong Inbox.
        int inserted = inboxRepository.claimEvent(
                event.eventId().toString(),
                event.orderId().toString(),
                TOPIC
        );

        if (inserted == 0) {
            return;
        }

        // 3. Tìm Order.
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy Order: " + event.orderId()
                ));

        // 4. Kết quả giải phóng phải đúng nội dung đơn.
        if (!Objects.equals(
                order.getProductId(),
                event.productId()
        )
                || !Objects.equals(
                order.getQuantity(),
                event.quantity()
        )) {

            throw new IllegalArgumentException(
                    "Kết quả release không khớp Order: "
                            + event.orderId()
            );
        }

        // 5. Domain quyết định chuyển trạng thái.
        boolean changed = order.completeCancelaion();

        // 6. Lưu bằng adapter hiện có, vẫn kiểm tra @Version.
        if (changed) {
            orderRepository.save(order);
        }
    }

    private void validate(StockReleasedEvent event) {

        if (event == null
                || event.eventId() == null
                || event.orderId() == null
                || event.productId() == null
                || event.productId() <= 0
                || event.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "StockReleasedEvent không hợp lệ"
            );
        }
    }
}