package dat.com.inventory2service.entity;

import dat.com.inventory2service.dto.ReleaseStockOutcome;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_release_commands",
        indexes = @Index(
                name = "idx_release_commands_order",
                columnList = "order_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReleaseCommandEntity {

    @Id
    @Column(name = "command_id", nullable = false, updatable = false)
    private UUID commandId;

    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(name = "result_event_id", nullable = false, updatable = false, unique = true)
    private UUID resultEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 30)
    private ReleaseStockOutcome outcome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
