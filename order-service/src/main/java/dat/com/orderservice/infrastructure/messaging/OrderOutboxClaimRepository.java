package dat.com.orderservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderOutboxClaimRepository {

    private final JdbcTemplate jdbcTemplate;


    //Nếu k dùng transaction thì khi select xong nó sẽ bỏ khóa sẽ bị lấy trùng khi woker kia đang xử lý
    @Transactional
    public List<OrderOutboxMessage> claimBatch() {

        // Một token dùng chung cho lần claim batch này.
        String token = UUID.randomUUID().toString();

        // 1. Chọn và khóa tối đa 5 sự kiện.
        List<OrderOutboxMessage> messages = jdbcTemplate.query(
                """
                SELECT id, BIN_TO_UUID(order_id) AS order_id, topic, payload
                FROM order_outbox
                WHERE (
                    status = 'NEW'
                    AND available_at <= CURRENT_TIMESTAMP(6)
                )
                OR (
                    status = 'PROCESSING'
                    AND locked_until < CURRENT_TIMESTAMP(6)
                )
                ORDER BY id
                LIMIT 5
                FOR UPDATE SKIP LOCKED
                """,
                (rs, rowNum) -> new OrderOutboxMessage(
                        rs.getLong("id"),
                        rs.getString("order_id"),
                        rs.getString("topic"),
                        rs.getString("payload"),
                        token
                )
        );

        if (messages.isEmpty()) {
            return messages;
        }

        // 2. Chuẩn bị placeholder cho các ID đã khóa.
        String placeholders = String.join(
                ",",
                java.util.Collections.nCopies(messages.size(), "?")
        );

        String sql = """
            UPDATE order_outbox
            SET status = 'PROCESSING',
                claim_token = ?,
                locked_until = TIMESTAMPADD(
                    SECOND, 60, CURRENT_TIMESTAMP(6)
                )
            WHERE id IN (%s)
            """.formatted(placeholders);

        // Tham số đầu là token, tiếp theo là các ID.
        Object[] params = new Object[messages.size() + 1];
        params[0] = token;

        for (int i = 0; i < messages.size(); i++) {
            params[i + 1] = messages.get(i).id();
        }

        // 3. Claim toàn bộ các bản ghi bằng một câu UPDATE.
        int updated = jdbcTemplate.update(sql, params);

        if (updated != messages.size()) {
            throw new IllegalStateException(
                    "Số Outbox được claim không khớp: selected="
                            + messages.size()
                            + ", updated="
                            + updated
            );
        }

        return messages;
    }

    public int markPublished(OrderOutboxMessage message) {

        return jdbcTemplate.update(
                """
                UPDATE order_outbox
                SET status = 'PUBLISHED',
                    published_at = CURRENT_TIMESTAMP(6),
                    locked_until = NULL,
                    claim_token = NULL
                WHERE id = ?
                  AND claim_token = ?
                  AND status = 'PROCESSING'
                """,
                message.id(),
                message.claimToken()
        );
    }

    public int markFailed(OrderOutboxMessage message) {

        return jdbcTemplate.update(
                """
                UPDATE order_outbox
                SET status = 'NEW',
                    retry_count = retry_count + 1,
                    available_at = TIMESTAMPADD(
                        SECOND, 10, CURRENT_TIMESTAMP(6)
                    ),
                    locked_until = NULL,
                    claim_token = NULL
                WHERE id = ?
                  AND claim_token = ?
                  AND status = 'PROCESSING'
                """,
                message.id(),
                message.claimToken()
        );
    }
}
