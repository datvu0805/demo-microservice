package dat.com.orderservice.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        Integer stockQuantity
) {
}
