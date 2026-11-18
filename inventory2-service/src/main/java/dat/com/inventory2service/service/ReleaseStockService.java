package dat.com.inventory2service.service;

import dat.com.inventory2service.dto.ReleaseStockCommand;
import dat.com.inventory2service.entity.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReleaseStockService {

    private final JdbcTemplate jdbcTemplate;
    private final OutboxPersistenceService outboxPersistenceService;

    private record ReservationRow(
            Long productId,
            int quantity,
            ReservationStatus status
    ) {
    }

    @Transactional
    public ReservationStatus release(ReleaseStockCommand command) {

        validate(command);

        // 1. Nếu release đến trước reserve:
        // ghi nhớ rằng Order này đã được giải phóng/hủy giữ kho.
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO inventory_reservations (
                    order_id,
                    product_id,
                    quantity,
                    status
                )
                VALUES (?, ?, ?, 'RELEASED')
                ON CONFLICT (order_id) DO NOTHING
                """,
                command.orderId(),
                command.productId(),
                command.quantity()
        );

        if (inserted == 1) {
            saveReleasedEvent(command);
            return ReservationStatus.RELEASED;
        }

        // 2. Reservation đã có: khóa để hai release
        // không cùng giảm reserved_quantity.
        ReservationRow existing = jdbcTemplate.queryForObject(
                """
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

        if (existing == null) {
            throw new IllegalStateException(
                    "Không tìm thấy reservation sau khi xảy ra conflict"
            );
        }

        // 3. Cùng Order phải khớp nội dung reservation.
        if (!Objects.equals(
                existing.productId(),
                command.productId()
        )
                || existing.quantity() != command.quantity()) {

            throw new IllegalArgumentException(
                    "Yêu cầu release không khớp reservation của Order: "
                            + command.orderId()
            );
        }

        // 4. Đã giải phóng: không trừ kho và không tạo event mới.
        if (existing.status() == ReservationStatus.RELEASED) {
            return ReservationStatus.RELEASED;
        }

        // 5. Chỉ reservation thực sự giữ hàng mới giảm reserved.
        if (existing.status() == ReservationStatus.RESERVED) {

            int updated = jdbcTemplate.update(
                    """
                    UPDATE inventory
                    SET reserved_quantity = reserved_quantity - ?
                    WHERE product_id = ?
                      AND reserved_quantity >= ?
                    """,
                    existing.quantity(),
                    existing.productId(),
                    existing.quantity()
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "Dữ liệu kho không nhất quán khi release Order: "
                                + command.orderId()
                );
            }

        } else if (existing.status() != ReservationStatus.REJECTED) {

            throw new IllegalStateException(
                    "Không thể release reservation ở trạng thái: "
                            + existing.status()
            );
        }

        // 6. Chuyển trạng thái reservation.
        int changed = jdbcTemplate.update(
                """
                UPDATE inventory_reservations
                SET status = 'RELEASED'
                WHERE order_id = ?
                  AND status = ?
                """,
                command.orderId(),
                existing.status().name()
        );

        if (changed != 1) {
            throw new IllegalStateException(
                    "Không thể cập nhật trạng thái reservation"
            );
        }

        // 7. Ghi sự kiện kết quả trong cùng transaction.
        saveReleasedEvent(command);

        return ReservationStatus.RELEASED;
    }

    private void saveReleasedEvent(ReleaseStockCommand command) {
        outboxPersistenceService.saveStockResult(
                command.orderId(),
                command.productId(),
                command.quantity(),
                "stock.released"
        );
    }

    private void validate(ReleaseStockCommand command) {
        if (command == null
                || command.eventId() == null
                || command.orderId() == null
                || command.productId() == null
                || command.productId() <= 0
                || command.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "ReleaseStockCommand không hợp lệ"
            );
        }
    }
}