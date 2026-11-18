package dat.com.orderservice.dto;

import java.util.UUID;

public record ReservationSnapshotResponse(
        UUID orderId,
        Long productId,
        Integer quantity,
        InventoryReservationStatus status
) {
}