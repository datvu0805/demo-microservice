package dat.com.inventory2service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        Long productId,
        int quantity,
        BigDecimal price
) {
}
