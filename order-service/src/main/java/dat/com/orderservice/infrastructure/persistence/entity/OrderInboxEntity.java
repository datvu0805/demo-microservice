package dat.com.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_event_inbox")
@Getter
@NoArgsConstructor
public class OrderInboxEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static OrderInboxEntity create(
            UUID eventId,
            Long orderId,
            String topic
    ) {
        OrderInboxEntity inbox = new OrderInboxEntity();

        inbox.eventId = eventId;
        inbox.orderId = orderId;
        inbox.topic = topic;
        inbox.processedAt = Instant.now();

        return inbox;
    }
}