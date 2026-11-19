package dat.com.orderservice.application.event;

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

    public void validate() {
        if (eventId == null
                || sagaId == null
                || commandId == null
                || orderId == null
                || productId == null
                || productId <= 0
                || quantity <= 0
                || outcome == null) {

            throw new IllegalArgumentException(
                    "ReserveStockResult không hợp lệ"
            );
        }
    }
}