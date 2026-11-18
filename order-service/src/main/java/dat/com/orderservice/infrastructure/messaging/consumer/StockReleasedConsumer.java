package dat.com.orderservice.infrastructure.messaging;

import dat.com.orderservice.application.event.StockReleasedEvent;
import dat.com.orderservice.application.usecase.ProcessStockReleasedUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReleasedConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessStockReleasedUseCase processStockReleasedUseCase;

    @KafkaListener(
            topics = "stock.released",
            groupId = "order-saga-group"
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

        // Validation đầy đủ nằm trong use case.
        processStockReleasedUseCase.execute(event);

        log.info(
                "Handled stock.released: eventId={}, orderId={}, "
                        + "partition={}, offset={}",
                event.eventId(),
                event.orderId(),
                record.partition(),
                record.offset()
        );
    }
}