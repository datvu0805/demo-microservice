package dat.com.orderservice.infrastructure.messaging.consumer;

import dat.com.orderservice.application.event.ReserveStockResult;
import dat.com.orderservice.application.saga.OrderSagaOrchestrator;
import dat.com.orderservice.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReserveStockResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderSagaOrchestrator orchestrator;

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_RESERVE_RESULT,
            groupId = "order-orchestration-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        ReserveStockResult result = objectMapper.readValue(
                record.value(),
                ReserveStockResult.class
        );

        orchestrator.onReserveResult(result);

        log.info(
                "Reserve result committed: eventId={}, sagaId={}, orderId={}",
                result.eventId(),
                result.sagaId(),
                result.orderId()
        );
    }
}