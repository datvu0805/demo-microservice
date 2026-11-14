package dat.com.inventory2service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_outbox",
        indexes = {
                @Index(
                        name = "idx_inventory_outbox_status",
                        columnList = "status,id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            unique = true
    )
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 100)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static InventoryOutboxEntity create(
            UUID eventId,
            UUID orderId,
            String topic,
            String payload
    ) {
        InventoryOutboxEntity entity =
                new InventoryOutboxEntity();
        entity.eventId = eventId;
        entity.orderId = orderId;
        entity.topic = topic;
        entity.payload = payload;
        entity.status = OutboxStatus.NEW;
        entity.createdAt = Instant.now();

        return entity;
    }
}
