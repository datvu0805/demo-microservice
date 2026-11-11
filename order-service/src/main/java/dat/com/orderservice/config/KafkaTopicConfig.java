package dat.com.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_CREATED ="order.created";

    @Bean
    public NewTopic orderCreateTopic(){

        return TopicBuilder
                .name(ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
