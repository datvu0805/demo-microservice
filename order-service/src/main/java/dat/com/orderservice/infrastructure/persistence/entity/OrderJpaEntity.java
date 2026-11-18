package dat.com.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(
                        name = "idx_orders_status_changed",
                        columnList = "status,status_changed_at"
                )
        }
)@Getter
@Setter
@NoArgsConstructor
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 30)
    private String status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @PrePersist
    public void initializeTimestamps() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (statusChangedAt == null) {
            statusChangedAt = now;
        }
    }
}
