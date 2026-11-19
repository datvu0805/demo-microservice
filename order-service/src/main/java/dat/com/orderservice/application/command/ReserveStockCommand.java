package dat.com.orderservice.application.command;

import java.util.UUID;

public record ReserveStockCommand(
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity
) {
}