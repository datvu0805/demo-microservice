package dat.com.inventory2service.kafka;

import dat.com.inventory2service.dto.OrderCreatedEvent;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.service.ReserveStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final ReserveStockService reserveStockService;

    @KafkaListener(topics = "order.created",groupId = "inventory-service-group")
    public void consume(ConsumerRecord<String , OrderCreatedEvent> record){
        OrderCreatedEvent event = record.value();
        log.info(
                "Received OrderCreatedEvent: eventId={}, orderId={}, productId={}, quantity={}",
                event.eventId(),
                event.orderId(),
                event.productId(),
                event.quantity()
        );

        ReservationStatus status = reserveStockService.reserve(event.orderId(), event.productId(), event.quantity());

        log.info(
                "Reservation completed: orderId={}, status={}",
                event.orderId(),
                status
        );
    }
}
