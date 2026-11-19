package dat.com.orderservice.application.command;

import java.util.UUID;

public record ReleaseStockCommand(
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity
) {
}