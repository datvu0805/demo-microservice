package dat.com.inventory2service.kafka.messaging;

import java.util.UUID;

public record OutboxMessage(
        Long id,
        UUID orderId,
        String topic,
        String payload,
        UUID claimToken
) {}
