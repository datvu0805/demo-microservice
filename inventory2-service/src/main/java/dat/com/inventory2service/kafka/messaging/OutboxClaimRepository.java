package dat.com.inventory2service.kafka.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxClaimRepository {

    private final JdbcTemplate jdbcTemplate;

    //Tìm 20 event để xử lý ,claim bằng cách cập nhật status và token

    //claimToken(): xác định xem worker nào có quyền truy cập để xử lý event

    //WHERE status = 'NEW'
    //                   OR (
    //                       status = 'PROCESSING'
    //                       AND locked_until < NOW()
    //                   )-> lấy những status đã hết hạn va đang lease lock ở trạng thái processing hoặc new,
    //                          đang processing nhưng vẫn đang trong thời hạn lock thì vẫn k đc lấy.
    //
    // locked_until = NOW() + INTERVAL '60 seconds',-> dặt thời hạn xử lý ,
    //  nếu worker đó chết thì worker khác thấy lock_until hết hạn thì sẽ xử lý
    //
    public List<OutboxMessage> claimBatch() {

        UUID token = UUID.randomUUID();

        return jdbcTemplate.query("""
            WITH selected AS (
                SELECT id
                FROM inventory_outbox
                WHERE status = 'NEW'
                   OR (
                       status = 'PROCESSING'
                       AND locked_until < NOW()
                   )
                ORDER BY id
                LIMIT 20
                FOR UPDATE SKIP LOCKED
            )
            UPDATE inventory_outbox o
            SET status = 'PROCESSING',
                locked_until = NOW() + INTERVAL '60 seconds',
                claim_token = ?
            FROM selected s
            WHERE o.id = s.id
            RETURNING
                o.id,
                o.order_id,
                o.topic,
                o.payload::text AS payload,
                o.claim_token
            """,
                (rs, rowNum) -> new OutboxMessage(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("topic"),
                        rs.getString("payload"),
                        rs.getObject("claim_token", UUID.class)
                ),
                token
        );
    }


    //vẫn phải lưu token vì khi xử lý chậm khiến token hết hạn , bị worker khác claim thì tránh bị tranh chấp
    public int markPublished(OutboxMessage message) {

        return jdbcTemplate.update("""
            UPDATE inventory_outbox
            SET status = 'PUBLISHED',
                published_at = NOW(),
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

    public int markFailed(OutboxMessage message) {

        return jdbcTemplate.update("""
            UPDATE inventory_outbox
            SET status = 'NEW',
                retry_count = retry_count + 1,
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