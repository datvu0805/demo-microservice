package dat.com.orderservice.dto;

import java.util.UUID;

public record ReleaseStockCommand(
        UUID eventId,
        UUID orderId,
        Long productId,
        int quantity
) {
}