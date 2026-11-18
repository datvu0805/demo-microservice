package dat.com.orderservice.application.idempotency;

import java.util.UUID;

public record OrderRequestClaim(
        String payloadHash,
        String claimToken,
        UUID orderId,
        boolean owned
) {
}