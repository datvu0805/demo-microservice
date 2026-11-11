package dat.com.inventory2service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OutboxKafkaProducerConfig {

    @Bean("outboxKafkaTemplate")
    public KafkaTemplate<String, String> outboxKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String servers
    ) {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                servers
        );

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        //ack=0, k đợi broker xác nhận message
        //      =1 leader ghi  nhận rồi phản hồi , k chờ các replica khác
        //   all: Đợi tất cả replica xác nhận hết
        config.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        //tránh trùng khi producer retry tự động
        config.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        ProducerFactory<String, String> factory =
                new DefaultKafkaProducerFactory<>(config);

        return new KafkaTemplate<>(factory);
    }
}