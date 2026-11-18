package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.application.idempotency.OrderRequestClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRequestClaimRepository {

    private final JdbcTemplate jdbcTemplate;

    //Propagation.MANDATORY  bắt buộc nó phải chạy trong một transaction có sẵn , nếu k có sẽ ném ex

    @Transactional(propagation = Propagation.MANDATORY)
    public OrderRequestClaim claim(
            String idempotencyKey,
            String payloadHash
    ) {

        // Token riêng cho lần thử claim này.
        String token = UUID.randomUUID().toString();

        // Key mới: insert.
        // Key đã có: giữ nguyên hash, token và order_id.
        jdbcTemplate.update(
                """
                INSERT INTO order_request_idempotency (
                    idempotency_key,
                    payload_hash,
                    claim_token,
                    order_id,
                    created_at
                )
                VALUES (?, ?, ?, NULL, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    idempotency_key = idempotency_key
                """,
                idempotencyKey,
                payloadHash,
                token
        );

        // Đọc UUID dạng chuỗi để ánh xạ sang java.util.UUID.
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    payload_hash,
                    claim_token,
                    BIN_TO_UUID(order_id) AS order_id
                FROM order_request_idempotency
                WHERE idempotency_key = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> {
                    String storedToken =
                            rs.getString("claim_token");

                    String rawOrderId =
                            rs.getString("order_id");

                    UUID orderId = rawOrderId == null
                            ? null
                            : UUID.fromString(rawOrderId);

                    return new OrderRequestClaim(
                            rs.getString("payload_hash"),
                            storedToken,
                            orderId,
                            token.equals(storedToken)
                    );
                },
                idempotencyKey
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            String idempotencyKey,
            String claimToken,
            UUID orderId
    ) {

        if (orderId == null) {
            throw new IllegalArgumentException(
                    "Order ID không được null"
            );
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE order_request_idempotency
                SET order_id = UUID_TO_BIN(?)
                WHERE idempotency_key = ?
                  AND claim_token = ?
                  AND order_id IS NULL
                """,
                orderId.toString(),
                idempotencyKey,
                claimToken
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Không thể hoàn tất idempotency key"
            );
        }
    }
}