package dat.com.orderservice.infrastructure.messaging;

import dat.com.orderservice.config.KafkaTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OrderOutboxWorker {

    private static final Set<String> SUPPORTED_TOPICS = Set.of(
            KafkaTopicConfig.ORDER_CREATED,
            KafkaTopicConfig.INVENTORY_RELEASE_REQUESTED
    );

    private final OrderOutboxClaimRepository claimRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderOutboxWorker(
            OrderOutboxClaimRepository claimRepository,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.claimRepository = claimRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(
            fixedDelayString = "${order.outbox.poll-delay-ms:1000}"
    )
    public void publishPendingEvent() {

        List<OrderOutboxMessage> messages =
                claimRepository.claimBatch();

        for (OrderOutboxMessage message : messages) {
            try {
                if (!SUPPORTED_TOPICS.contains(message.topic())) {
                    throw new IllegalArgumentException(
                            "Topic không được hỗ trợ: " + message.topic()
                    );
                }

                // orderId trong OrderOutboxMessage hiện là String.
                kafkaTemplate.send(
                        message.topic(),
                        message.orderId(),
                        message.payload()
                ).get(5, TimeUnit.SECONDS);

                int updated = claimRepository.markPublished(message);

                if (updated == 1) {
                    log.info(
                            "Outbox published: id={}, orderId={}, topic={}",
                            message.id(),
                            message.orderId(),
                            message.topic()
                    );
                } else {
                    log.warn(
                            "Outbox claim lost: id={}",
                            message.id()
                    );
                }

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();

                log.warn(
                        "Outbox worker interrupted: id={}",
                        message.id()
                );

                // Lease hết hạn thì worker có thể claim lại.
                return;

            } catch (Exception ex) {
                log.error(
                        "Outbox publish failed: id={}, topic={}",
                        message.id(),
                        message.topic(),
                        ex
                );

                claimRepository.markFailed(message);
            }
        }
    }
}