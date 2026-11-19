package dat.com.inventory2service.kafka;

import dat.com.inventory2service.dto.OrchestratedReleaseStockCommand;
import dat.com.inventory2service.service.ProcessOrchestratedReleaseStockCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratedReleaseStockCommandConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessOrchestratedReleaseStockCommandService service;

    @KafkaListener(
            topics = "inventory.release.command",
            groupId = "inventory-release-orchestration-group",
            containerFactory = "releaseStockKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException(
                    "Orchestrated release command payload bị rỗng"
            );
        }

        OrchestratedReleaseStockCommand command = objectMapper.readValue(
                record.value(),
                OrchestratedReleaseStockCommand.class
        );
        command.validate();

        var result = service.execute(command);

        log.info(
                "Orchestrated release command committed: commandId={}, sagaId={}, orderId={}, eventId={}",
                result.commandId(), result.sagaId(), result.orderId(), result.eventId()
        );
    }
}
