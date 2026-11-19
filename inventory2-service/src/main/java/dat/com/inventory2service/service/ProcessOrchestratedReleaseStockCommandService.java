package dat.com.inventory2service.service;

import dat.com.inventory2service.config.InventorySagaTopicConfig;
import dat.com.inventory2service.dto.OrchestratedReleaseStockCommand;
import dat.com.inventory2service.dto.ReleaseCommandClaim;
import dat.com.inventory2service.dto.ReleaseStockCommand;
import dat.com.inventory2service.dto.ReleaseStockOutcome;
import dat.com.inventory2service.dto.ReleaseStockResult;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.repository.ReleaseCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcessOrchestratedReleaseStockCommandService {

    private final ReleaseCommandRepository commandRepository;
    private final ReleaseStockService releaseStockService;
    private final OutboxPersistenceService outboxPersistenceService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ReleaseStockResult execute(OrchestratedReleaseStockCommand command) {
        Objects.requireNonNull(command, "command không được null");
        command.validate();

        ReleaseCommandClaim claim = commandRepository.claim(command);
        if (!claim.owned()) {
            ReleaseStockResult result = claim.toResult();
            requeuePublishedResult(result);
            return result;
        }

        ReservationStatus reservationStatus = releaseStockService.release(new ReleaseStockCommand(
                command.commandId(),
                command.orderId(),
                command.productId(),
                command.quantity()
        ));
        if (reservationStatus != ReservationStatus.RELEASED) {
            throw new IllegalStateException(
                    "Inventory release chưa hoàn tất: " + reservationStatus
            );
        }

        ReleaseStockResult result = new ReleaseStockResult(
                claim.resultEventId(),
                claim.sagaId(),
                claim.commandId(),
                claim.orderId(),
                claim.productId(),
                claim.quantity(),
                ReleaseStockOutcome.RELEASED
        );
        commandRepository.complete(claim, result.outcome());
        outboxPersistenceService.saveReleaseResult(result);
        return result;
    }

    private void requeuePublishedResult(ReleaseStockResult result) {
        OutboxRow row = jdbcTemplate.queryForObject("""
                SELECT id, status
                FROM inventory_outbox
                WHERE event_id = ? AND order_id = ? AND topic = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new OutboxRow(
                        rs.getLong("id"), rs.getString("status")
                ),
                result.eventId(), result.orderId(),
                InventorySagaTopicConfig.RELEASE_RESULT
        );
        if (row == null) {
            throw new IllegalStateException(
                    "Release command đã hoàn tất nhưng thiếu Outbox kết quả"
            );
        }

        switch (row.status()) {
            case "NEW", "PROCESSING" -> { }
            case "PUBLISHED" -> {
                int updated = jdbcTemplate.update("""
                        UPDATE inventory_outbox
                        SET status = 'NEW', published_at = NULL, locked_until = NULL
                        WHERE id = ? AND status = 'PUBLISHED'
                        """, row.id());
                if (updated != 1) {
                    throw new IllegalStateException(
                            "Không thể lên lịch gửi lại release result"
                    );
                }
            }
            default -> throw new IllegalStateException(
                    "Trạng thái Outbox không hỗ trợ: " + row.status()
            );
        }
    }

    private record OutboxRow(Long id, String status) { }
}
