package dat.com.orderservice.application.event;

import java.util.UUID;

public record StockeResultEvent(
        UUID eventId,
        Long orderId,
        Long productId,
        int quantity
) {
}
