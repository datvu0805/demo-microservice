package dat.com.inventory2service.dto;

import java.util.UUID;

public record ReleaseStockResult(
        UUID eventId,
        UUID sagaId,
        UUID commandId,
        UUID orderId,
        Long productId,
        int quantity,
        ReleaseStockOutcome outcome
) {
}
