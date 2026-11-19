package dat.com.inventory2service.dto;

import java.util.UUID;

public record ReserveStockCommand(
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity
) {

    public void validate() {
        if (commandId == null) {
            throw new IllegalArgumentException(
                    "ReserveStockCommand thiếu commandId"
            );
        }

        if (sagaId == null) {
            throw new IllegalArgumentException(
                    "ReserveStockCommand thiếu sagaId"
            );
        }

        if (orderId == null) {
            throw new IllegalArgumentException(
                    "ReserveStockCommand thiếu orderId"
            );
        }

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "productId không hợp lệ"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity phải lớn hơn 0"
            );
        }
    }
}