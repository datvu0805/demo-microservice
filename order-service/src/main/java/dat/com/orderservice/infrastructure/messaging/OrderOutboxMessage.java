package dat.com.orderservice.infrastructure.messaging;

public record OrderOutboxMessage(
        Long id,
        String orderId,
        String topic,
        String payload,
        String claimToken
) {
}
