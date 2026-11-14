package dat.com.orderservice.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedevent(
        UUID eventId,
        UUID orderId,
        Long productId,
        int quantity,
        BigDecimal price
) {

}
