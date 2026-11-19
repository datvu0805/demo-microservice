package dat.com.inventory2service.dto;

import dat.com.inventory2service.dto.ReserveStockOutcome;
import dat.com.inventory2service.dto.ReserveStockResult;

import java.util.UUID;

public record ReserveCommandClaim(
        boolean owned,
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity,
        UUID resultEventId,
        ReserveStockOutcome outcome
) {

    public ReserveStockResult toResult() {
        if (outcome == null) {
            throw new IllegalStateException(
                    "Command chưa có kết quả: " + commandId
            );
        }

        return new ReserveStockResult(
                resultEventId,
                sagaId,
                commandId,
                orderId,
                productId,
                quantity,
                outcome
        );
    }
}