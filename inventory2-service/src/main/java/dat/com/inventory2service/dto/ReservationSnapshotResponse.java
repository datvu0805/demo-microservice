package dat.com.inventory2service.dto;

import dat.com.inventory2service.entity.ReservationStatus;

import java.util.UUID;

public record ReservationSnapshotResponse(
        UUID orderId,
        Long productId,
        Integer quantity,
        ReservationStatus status
) {
}