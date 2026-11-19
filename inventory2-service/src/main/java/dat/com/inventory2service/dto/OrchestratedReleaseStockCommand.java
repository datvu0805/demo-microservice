package dat.com.inventory2service.dto;

import java.util.UUID;

public record OrchestratedReleaseStockCommand(
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity
) {
    public void validate() {
        if (commandId == null
                || sagaId == null
                || orderId == null
                || productId == null
                || productId <= 0
                || quantity <= 0) {
            throw new IllegalArgumentException(
                    "OrchestratedReleaseStockCommand không hợp lệ"
            );
        }
    }
}
