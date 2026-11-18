package dat.com.inventory2service.kafka;

import dat.com.inventory2service.dto.OrderCreatedEvent;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.service.ReserveStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final ReserveStockService reserveStockService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service-group",
            containerFactory = "releaseStockKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {

        log.info(
                "Received order.created: partition={}, offset={}, key={}",
                record.partition(),
                record.offset(),
                record.key()
        );

        OrderCreatedEvent event = objectMapper.readValue(
                record.value(),
                OrderCreatedEvent.class
        );

        if (event == null
                || event.eventId() == null
                || event.orderId() == null
                || event.productId() == null
                || event.productId() <= 0
                || event.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "OrderCreatedEvent không hợp lệ: "
                            + "partition=" + record.partition()
                            + ", offset=" + record.offset()
            );
        }

        ReservationStatus status = reserveStockService.reserve(
                event.orderId(),
                event.productId(),
                event.quantity()
        );

        log.info(
                "Reservation completed: eventId={}, orderId={}, status={}",
                event.eventId(),
                event.orderId(),
                status
        );
    }
}