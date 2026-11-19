package dat.com.inventory2service.service;

import dat.com.inventory2service.config.InventorySagaTopicConfig;
import dat.com.inventory2service.dto.ReserveStockCommand;
import dat.com.inventory2service.dto.ReserveStockOutcome;
import dat.com.inventory2service.dto.ReserveStockResult;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.dto.ReserveCommandClaim;
import dat.com.inventory2service.repository.ReserveCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessReserveStockCommandService {

    private final ReserveCommandRepository commandRepository;
    private final OutboxPersistenceService outboxPersistenceService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ReserveStockResult execute(ReserveStockCommand command) {
        Objects.requireNonNull(command, "command không được null");
        command.validate();

        // 1. Claim command và đối chiếu nội dung.
        ReserveCommandClaim claim = commandRepository.claim(command);

        // 2. Command đã hoàn tất: dùng kết quả cũ.
        if (!claim.owned()) {
            ReserveStockResult result = claim.toResult();

            requeuePublishedResult(result);

            return result;
        }

        // 3. Thực hiện nghiệp vụ giữ hàng.
        ReserveStockOutcome outcome = reserve(command);

        // 4. Lưu kết quả command.
        commandRepository.complete(claim, outcome);

        // 5. Dùng eventId đã sinh khi claim, không sinh lại.
        ReserveStockResult result = new ReserveStockResult(
                claim.resultEventId(),
                claim.sagaId(),
                claim.commandId(),
                claim.orderId(),
                claim.productId(),
                claim.quantity(),
                outcome
        );

        // 6. Ghi Outbox trong cùng transaction.
        outboxPersistenceService.saveReserveResult(result);

        return result;
    }

    private ReserveStockOutcome reserve(ReserveStockCommand command) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO inventory_reservations (
                    order_id,
                    product_id,
                    quantity,
                    status
                )
                VALUES (?, ?, ?, 'PROCESSING')
                ON CONFLICT (order_id) DO NOTHING
                """,
                command.orderId(),
                command.productId(),
                command.quantity()
        );

        ReservationRow reservation = jdbcTemplate.queryForObject("""
                SELECT product_id, quantity, status
                FROM inventory_reservations
                WHERE order_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new ReservationRow(
                        rs.getLong("product_id"),
                        rs.getInt("quantity"),
                        ReservationStatus.valueOf(
                                rs.getString("status")
                        )
                ),
                command.orderId()
        );

        if (reservation == null) {
            throw new IllegalStateException(
                    "Không tìm thấy reservation sau khi claim"
            );
        }

        if (!reservation.productId().equals(command.productId())
                || reservation.quantity() != command.quantity()) {
            throw new IllegalArgumentException(
                    "orderId đã có reservation với nội dung khác"
            );
        }

        // Reservation đã tồn tại: không giữ hàng lần nữa.
        if (inserted == 0) {
            return switch (reservation.status()) {
                case RESERVED -> ReserveStockOutcome.RESERVED;
                case REJECTED -> ReserveStockOutcome.REJECTED;
                case RELEASED -> ReserveStockOutcome.ALREADY_RELEASED;

                case PROCESSING -> throw new IllegalStateException(
                        "Reservation tồn tại nhưng chưa hoàn tất"
                );
            };
        }

        // Kiểm tra và giữ hàng trong cùng câu UPDATE.
        int updated = jdbcTemplate.update("""
                UPDATE inventory
                SET reserved_quantity = reserved_quantity + ?
                WHERE product_id = ?
                  AND quantity - reserved_quantity >= ?
                """,
                command.quantity(),
                command.productId(),
                command.quantity()
        );

        if (updated > 1) {
            throw new IllegalStateException(
                    "Có nhiều bản ghi Inventory cho một productId"
            );
        }

        ReservationStatus status = updated == 1
                ? ReservationStatus.RESERVED
                : ReservationStatus.REJECTED;

        int completed = jdbcTemplate.update("""
                UPDATE inventory_reservations
                SET status = ?
                WHERE order_id = ?
                  AND status = 'PROCESSING'
                """,
                status.name(),
                command.orderId()
        );

        if (completed != 1) {
            throw new IllegalStateException(
                    "Không thể hoàn tất reservation"
            );
        }

        return updated == 1
                ? ReserveStockOutcome.RESERVED
                : ReserveStockOutcome.REJECTED;
    }

    private void requeuePublishedResult(ReserveStockResult result) {
        OutboxRow row = jdbcTemplate.queryForObject("""
                SELECT id, status
                FROM inventory_outbox
                WHERE event_id = ?
                  AND order_id = ?
                  AND topic = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new OutboxRow(
                        rs.getLong("id"),
                        rs.getString("status")
                ),
                result.eventId(),
                result.orderId(),
                InventorySagaTopicConfig.RESERVE_RESULT
        );

        if (row == null) {
            throw new IllegalStateException(
                    "Command đã hoàn tất nhưng thiếu Outbox kết quả"
            );
        }

        switch (row.status()) {
            case "NEW", "PROCESSING" -> {
                // Đã chờ gửi hoặc đang thuộc quyền worker.
                // Không thay đổi lease hoặc quyền xử lý.
            }

            case "PUBLISHED" -> {
                int updated = jdbcTemplate.update("""
                        UPDATE inventory_outbox
                        SET status = 'NEW',
                            published_at = NULL,
                            locked_until = NULL
                        WHERE id = ?
                          AND status = 'PUBLISHED'
                        """,
                        row.id()
                );

                if (updated != 1) {
                    throw new IllegalStateException(
                            "Không thể lên lịch gửi lại kết quả"
                    );
                }
            }

            default -> throw new IllegalStateException(
                    "Trạng thái Outbox không hỗ trợ: " + row.status()
            );
        }
    }

    private record ReservationRow(
            Long productId,
            int quantity,
            ReservationStatus status
    ) {
    }

    private record OutboxRow(
            Long id,
            String status
    ) {
    }
}