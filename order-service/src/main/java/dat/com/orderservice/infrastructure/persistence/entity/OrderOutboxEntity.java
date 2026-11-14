package dat.com.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "order_outbox",
        indexes = {
                @Index(
                        name = "idx_order_outbox_poll",
                        columnList = "status,available_at,id"
                )
        }
)
@Getter
@NoArgsConstructor
public class OrderOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            length = 36
    )
    private String eventId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OrderOutboxEntity create(
            UUID eventId,
            UUID orderId,
            String topic,
            String payload
    ) {
        Instant now = Instant.now();

        OrderOutboxEntity outbox = new OrderOutboxEntity();

        outbox.eventId = eventId.toString();
        outbox.orderId = orderId;
        outbox.topic = topic;
        outbox.payload = payload;

        outbox.status = OrderOutboxStatus.NEW;
        outbox.retryCount = 0;
        outbox.availableAt = now;
        outbox.createdAt = now;

        return outbox;
    }
}
