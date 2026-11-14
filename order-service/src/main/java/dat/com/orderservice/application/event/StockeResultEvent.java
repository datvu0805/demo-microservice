package dat.com.orderservice.application.event;

import java.util.UUID;

public record StockeResultEvent(
        UUID eventId,
        UUID orderId,
        Long productId,
        int quantity
) {
}
