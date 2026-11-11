package dat.com.orderservice.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedevent(
        UUID eventID,
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal price
) {

}
