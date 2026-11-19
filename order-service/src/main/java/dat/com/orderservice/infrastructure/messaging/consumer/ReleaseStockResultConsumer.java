package dat.com.orderservice.infrastructure.messaging.consumer;

import dat.com.orderservice.application.event.ReleaseStockResult;
import dat.com.orderservice.application.saga.OrderSagaOrchestrator;
import dat.com.orderservice.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReleaseStockResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderSagaOrchestrator orchestrator;

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_RELEASE_RESULT,
            groupId = "order-orchestration-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException("ReleaseStockResult payload bị rỗng");
        }
        ReleaseStockResult result = objectMapper.readValue(
                record.value(), ReleaseStockResult.class
        );
        orchestrator.onReleaseResult(result);
        log.info(
                "Release result committed: eventId={}, sagaId={}, commandId={}, orderId={}",
                result.eventId(), result.sagaId(), result.commandId(), result.orderId()
        );
    }
}
