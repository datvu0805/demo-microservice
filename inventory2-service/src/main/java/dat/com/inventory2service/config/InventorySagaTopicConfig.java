package dat.com.inventory2service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class InventorySagaTopicConfig {

    public static final String RESERVE_REQUESTED =
            "inventory.reserve.requested";

    public static final String RESERVE_RESULT =
            "inventory.reserve.result";

    public static final String RELEASE_COMMAND =
            "inventory.release.command";

    public static final String RELEASE_RESULT =
            "inventory.release.result";

    @Bean
    public NewTopic inventoryReserveResultTopic() {
        return TopicBuilder
                .name(RESERVE_RESULT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReserveRequestedDltTopic() {
        return TopicBuilder
                .name(RESERVE_REQUESTED + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseCommandTopic() {
        return TopicBuilder
                .name(RELEASE_COMMAND)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseCommandDltTopic() {
        return TopicBuilder
                .name(RELEASE_COMMAND + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseResultTopic() {
        return TopicBuilder
                .name(RELEASE_RESULT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseResultDltTopic() {
        return TopicBuilder
                .name(RELEASE_RESULT + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
