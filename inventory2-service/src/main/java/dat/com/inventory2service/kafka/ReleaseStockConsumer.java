package dat.com.inventory2service.kafka;

import dat.com.inventory2service.dto.ReleaseStockCommand;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.service.ReleaseStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReleaseStockConsumer {

    private final ObjectMapper objectMapper;
    private final ReleaseStockService releaseStockService;

    @KafkaListener(
            topics = "inventory.release.requested",
            groupId = "inventory-release-group",
            containerFactory = "releaseStockKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {

        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException(
                    "ReleaseStockCommand payload bị rỗng"
            );
        }

        // 1. Chuyển JSON thành command.
        ReleaseStockCommand command = objectMapper.readValue(
                record.value(),
                ReleaseStockCommand.class
        );

        if (command == null) {
            throw new IllegalArgumentException(
                    "ReleaseStockCommand không được null"
            );
        }

        log.info(
                "Received release command: eventId={}, orderId={}, "
                        + "partition={}, offset={}",
                command.eventId(),
                command.orderId(),
                record.partition(),
                record.offset()
        );

        // 2. Gọi bean khác để thực hiện transaction release.
        ReservationStatus status = releaseStockService.release(command);

        // Transaction đã commit trước khi lời gọi trên trả về.
        log.info(
                "Release completed: eventId={}, orderId={}, status={}",
                command.eventId(),
                command.orderId(),
                status
        );
    }
}