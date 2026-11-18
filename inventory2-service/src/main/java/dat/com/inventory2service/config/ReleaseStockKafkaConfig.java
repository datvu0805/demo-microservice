package dat.com.inventory2service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReleaseStockKafkaConfig {

    @Bean
    public NewTopic releaseStockDltTopic() {
        return TopicBuilder
                .name("inventory.release.requested.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockReleasedTopic() {
        return TopicBuilder
                .name("stock.released")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder
                .name("order.created.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean("releaseStockKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    releaseStockKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate
    ) {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                servers
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(properties);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                record.topic() + ".DLT",
                                -1
                        )
                );

        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(2000L, 2L)
                );

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.RECORD
        );

        factory.setCommonErrorHandler(errorHandler);

        factory.setConcurrency(1);

        return factory;
    }
}