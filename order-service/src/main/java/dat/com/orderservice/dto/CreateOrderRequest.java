package dat.com.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Price is required")
        BigDecimal price,

        @NotNull(message = "Product name is required")
        String productName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity

) {}