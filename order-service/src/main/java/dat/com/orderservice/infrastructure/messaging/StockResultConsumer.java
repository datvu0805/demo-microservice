package dat.com.orderservice.infrastructure.messaging;

import dat.com.orderservice.application.event.StockeResultEvent;
import dat.com.orderservice.application.usecase.ProcessStockResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockResultConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessStockResultUseCase processStockResultUseCase;

    @KafkaListener(
            topics = {"stock.reserved", "stock.reject"},
            groupId = "order-saga-group"
    )
    public void consume(ConsumerRecord<String,String> record){
        //1.chuyển json nhận từ kafka thành java record
        StockeResultEvent event = objectMapper.readValue(record.value(),StockeResultEvent.class);

        //2.ghi nhận thông tin phục vụ theo dõi và debug
        log.info(
                "Received stock result: topic={}, partition={}, offset={}, "
                        + "eventId={}, orderId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                event.eventId(),
                event.orderId()
        );

        //3.Gọi sang processStockResultUseCase để xử lý
        processStockResultUseCase.execute(event,record.topic());

        log.info(
                "Handled stock result: eventId={}, orderId={}",
                event.eventId(),
                event.orderId());
    }
}
