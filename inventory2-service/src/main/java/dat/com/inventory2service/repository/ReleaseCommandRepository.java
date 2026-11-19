package dat.com.inventory2service.repository;

import dat.com.inventory2service.dto.OrchestratedReleaseStockCommand;
import dat.com.inventory2service.dto.ReleaseCommandClaim;
import dat.com.inventory2service.dto.ReleaseStockOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReleaseCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public ReleaseCommandClaim claim(OrchestratedReleaseStockCommand command) {
        Objects.requireNonNull(command, "command không được null");
        command.validate();

        UUID resultEventId = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO inventory_release_commands (
                    command_id, saga_id, order_id, product_id, quantity,
                    result_event_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (command_id) DO NOTHING
                """,
                command.commandId(), command.sagaId(), command.orderId(),
                command.productId(), command.quantity(), resultEventId
        );

        ReleaseCommandClaim claim = jdbcTemplate.queryForObject("""
                SELECT command_id, saga_id, order_id, product_id, quantity,
                       result_event_id, outcome
                FROM inventory_release_commands
                WHERE command_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> {
                    String outcome = rs.getString("outcome");
                    return new ReleaseCommandClaim(
                            inserted == 1,
                            rs.getObject("command_id", UUID.class),
                            rs.getObject("saga_id", UUID.class),
                            rs.getObject("order_id", UUID.class),
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getObject("result_event_id", UUID.class),
                            outcome == null ? null : ReleaseStockOutcome.valueOf(outcome)
                    );
                },
                command.commandId()
        );

        if (claim == null) {
            throw new IllegalStateException("Không đọc được release command sau khi claim");
        }
        if (!claim.sagaId().equals(command.sagaId())
                || !claim.orderId().equals(command.orderId())
                || !claim.productId().equals(command.productId())
                || claim.quantity() != command.quantity()) {
            throw new IllegalArgumentException(
                    "commandId đã được dùng cho nội dung release khác: "
                            + command.commandId()
            );
        }
        if (!claim.owned() && claim.outcome() == null) {
            throw new IllegalStateException(
                    "Release command tồn tại nhưng chưa có kết quả: "
                            + command.commandId()
            );
        }
        return claim;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(ReleaseCommandClaim claim, ReleaseStockOutcome outcome) {
        Objects.requireNonNull(claim, "claim không được null");
        Objects.requireNonNull(outcome, "outcome không được null");
        if (!claim.owned()) {
            throw new IllegalStateException(
                    "Không được ghi đè kết quả release command đã tồn tại"
            );
        }
        int updated = jdbcTemplate.update("""
                UPDATE inventory_release_commands
                SET outcome = ?, completed_at = CURRENT_TIMESTAMP
                WHERE command_id = ? AND result_event_id = ? AND outcome IS NULL
                """,
                outcome.name(), claim.commandId(), claim.resultEventId()
        );
        if (updated != 1) {
            throw new IllegalStateException(
                    "Không thể hoàn tất release command: " + claim.commandId()
            );
        }
    }
}
