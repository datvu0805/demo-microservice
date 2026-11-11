package dat.com.inventory2service.kafka.messaging;

import java.util.UUID;

public record OutboxMessage(
        Long id,
        Long orderId,
        String topic,
        String payload,
        UUID claimToken
) {}