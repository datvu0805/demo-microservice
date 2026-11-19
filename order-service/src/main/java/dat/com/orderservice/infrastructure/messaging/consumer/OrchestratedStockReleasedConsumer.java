package dat.com.orderservice.infrastructure.messaging.consumer;

import dat.com.orderservice.application.event.StockReleasedEvent;
import dat.com.orderservice.application.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratedStockReleasedConsumer {

    private final ObjectMapper objectMapper;
    private final OrderSagaOrchestrator orchestrator;

    @KafkaListener(
            topics = "stock.released",
            groupId = "order-orchestration-release-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException(
                    "StockReleasedEvent payload bị rỗng"
            );
        }

        StockReleasedEvent event = objectMapper.readValue(
                record.value(),
                StockReleasedEvent.class
        );
        orchestrator.onStockReleased(event);

        log.info(
                "Orchestrated release result handled: eventId={}, orderId={}, offset={}",
                event.eventId(),
                event.orderId(),
                record.offset()
        );
    }
}
