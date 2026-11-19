package dat.com.inventory2service.repository;
import dat.com.inventory2service.dto.ReserveCommandClaim;
import dat.com.inventory2service.dto.ReserveStockCommand;
import dat.com.inventory2service.dto.ReserveStockOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReserveCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public ReserveCommandClaim claim(
            ReserveStockCommand command
    ) {
        Objects.requireNonNull(command, "command không được null");
        command.validate();

        UUID resultEventId = UUID.randomUUID();

        int inserted = jdbcTemplate.update("""
        INSERT INTO inventory_reserve_commands (
            command_id,
            saga_id,
            order_id,
            product_id,
            quantity,
            result_event_id,
            created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (command_id) DO NOTHING
        """,
                command.commandId(),
                command.sagaId(),
                command.orderId(),
                command.productId(),
                command.quantity(),
                resultEventId
        );

        ReserveCommandClaim claim = jdbcTemplate.queryForObject("""
                SELECT
                    command_id,
                    saga_id,
                    order_id,
                    product_id,
                    quantity,
                    result_event_id,
                    outcome
                FROM inventory_reserve_commands
                WHERE command_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> {
                    String outcome = rs.getString("outcome");

                    return new ReserveCommandClaim(
                            inserted == 1,
                            rs.getObject("command_id", UUID.class),
                            rs.getObject("saga_id", UUID.class),
                            rs.getObject("order_id", UUID.class),
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getObject("result_event_id", UUID.class),
                            outcome == null
                                    ? null
                                    : ReserveStockOutcome.valueOf(outcome)
                    );
                },
                command.commandId()
        );

        if (claim == null) {
            throw new IllegalStateException(
                    "Không đọc được command sau khi claim"
            );
        }

        verifySameRequest(claim, command);

        if (!claim.owned() && claim.outcome() == null) {
            throw new IllegalStateException(
                    "Command đã tồn tại nhưng chưa có kết quả: "
                            + command.commandId()
            );
        }

        return claim;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            ReserveCommandClaim claim,
            ReserveStockOutcome outcome
    ) {
        Objects.requireNonNull(claim, "claim không được null");
        Objects.requireNonNull(outcome, "outcome không được null");

        if (!claim.owned()) {
            throw new IllegalStateException(
                    "Không được ghi đè kết quả command đã tồn tại"
            );
        }

        int updated = jdbcTemplate.update("""
                UPDATE inventory_reserve_commands
                SET outcome = ?,
                    completed_at = CURRENT_TIMESTAMP
                WHERE command_id = ?
                  AND result_event_id = ?
                  AND outcome IS NULL
                """,
                outcome.name(),
                claim.commandId(),
                claim.resultEventId()
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Không thể hoàn tất command: "
                            + claim.commandId()
            );
        }
    }

    private void verifySameRequest(
            ReserveCommandClaim claim,
            ReserveStockCommand command
    ) {
        boolean sameRequest =
                claim.sagaId().equals(command.sagaId())
                        && claim.orderId().equals(command.orderId())
                        && claim.productId().equals(command.productId())
                        && claim.quantity() == command.quantity();

        if (!sameRequest) {
            throw new IllegalArgumentException(
                    "commandId đã được dùng cho nội dung khác: "
                            + command.commandId()
            );
        }
    }
}