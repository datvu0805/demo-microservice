package dat.com.inventory2service.kafka;

import dat.com.inventory2service.config.InventorySagaTopicConfig;
import dat.com.inventory2service.dto.ReserveStockCommand;
import dat.com.inventory2service.dto.ReserveStockResult;
import dat.com.inventory2service.service.ProcessReserveStockCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReserveStockCommandConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessReserveStockCommandService service;

    @KafkaListener(
            topics = InventorySagaTopicConfig.RESERVE_REQUESTED,
            groupId = "inventory-reserve-command-group",
            containerFactory = "releaseStockKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info(
                "Received reserve command: partition={}, offset={}, key={}",
                record.partition(),
                record.offset(),
                record.key()
        );

        ReserveStockCommand command = objectMapper.readValue(
                record.value(),
                ReserveStockCommand.class
        );

        ReserveStockResult result = service.execute(command);

        log.info(
                "Reserve command committed: commandId={}, sagaId={}, "
                        + "orderId={}, outcome={}",
                result.commandId(),
                result.sagaId(),
                result.orderId(),
                result.outcome()
        );
    }
}