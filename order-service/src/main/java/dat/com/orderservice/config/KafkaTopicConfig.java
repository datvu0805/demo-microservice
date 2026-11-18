package dat.com.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_CREATED ="order.created";
    public static final String INVENTORY_RELEASE_REQUESTED =
            "inventory.release.requested";

    @Bean
    public NewTopic inventoryReleaseRequestedTopic() {
        return TopicBuilder
                .name(INVENTORY_RELEASE_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic stockReleasedDlt() {
        return TopicBuilder
                .name("stock.released.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic orderCreateTopic(){

        return TopicBuilder
                .name(ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
