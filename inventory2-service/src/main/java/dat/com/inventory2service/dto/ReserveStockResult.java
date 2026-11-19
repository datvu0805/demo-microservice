package dat.com.inventory2service.dto;

import java.util.UUID;

public record ReserveStockResult(
        UUID eventId,
        UUID sagaId,
        UUID commandId,
        UUID orderId,
        Long productId,
        int quantity,
        ReserveStockOutcome outcome
) {
}