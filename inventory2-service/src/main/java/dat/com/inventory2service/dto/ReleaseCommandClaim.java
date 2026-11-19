package dat.com.inventory2service.dto;

import java.util.UUID;

public record ReleaseCommandClaim(
        boolean owned,
        UUID commandId,
        UUID sagaId,
        UUID orderId,
        Long productId,
        int quantity,
        UUID resultEventId,
        ReleaseStockOutcome outcome
) {
    public ReleaseStockResult toResult() {
        if (outcome == null) {
            throw new IllegalStateException("Release command chưa có kết quả");
        }
        return new ReleaseStockResult(
                resultEventId, sagaId, commandId, orderId,
                productId, quantity, outcome
        );
    }
}
