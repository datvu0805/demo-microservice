package dat.com.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "order_request_idempotency")
@Getter
@NoArgsConstructor
public class OrderRequestIdempotencyEntity {

    @Id
    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String idempotencyKey;

    @Column(
            name = "payload_hash",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String payloadHash;

    @Column(
            name = "claim_token",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String claimToken;

    @Column(name = "order_id")
    private Long orderId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}