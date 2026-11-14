package dat.com.orderservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class OrderKafkaErrorConfig {

    @Bean
    public NewTopic stockReservedDlt() {
        return TopicBuilder.name("stock.reserved.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockRejectDlt() {
        return TopicBuilder.name("stock.reject.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler orderKafkaErrorHandler(
            @Qualifier("dltKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                record.topic() + ".DLT",
                                -1
                        )
                );

        // Nếu gửi DLT thất bại, recovery cũng phải báo thất bại.
        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2000L, 2L)
        );

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> log.warn(
                        "Stock result failed: topic={}, partition={}, "
                                + "offset={}, attempt={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        deliveryAttempt,
                        exception
                )
        );

        return errorHandler;
    }
}