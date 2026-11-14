package dat.com.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_event_inbox")
@Getter
@NoArgsConstructor
public class OrderInboxEntity {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static OrderInboxEntity create(
            UUID eventId,
            UUID orderId,
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
