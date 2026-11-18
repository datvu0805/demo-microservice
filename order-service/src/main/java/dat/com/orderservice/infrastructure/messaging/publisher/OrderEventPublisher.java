package dat.com.orderservice.infrastructure.messaging.publisher;

import dat.com.orderservice.application.event.OrderCreatedevent;
import dat.com.orderservice.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

private final @Qualifier("orderKafkaTemplate")
        KafkaTemplate<String, OrderCreatedevent> kafkaTemplate;

    public CompletableFuture<Void> publish(OrderCreatedevent event){
        String key = event.orderId().toString();

        return kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED,key,event)
                .thenAccept(result ->{
                    log.info( "Published OrderCreated: orderId={}, partition={}, offset={}",
                            event.orderId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
