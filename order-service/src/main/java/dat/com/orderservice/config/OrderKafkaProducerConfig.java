package dat.com.orderservice.config;


import dat.com.orderservice.application.event.OrderCreatedevent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderKafkaProducerConfig {

    private Map<String, Object> producerProperties(String servers) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                servers
        );

        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        return properties;
    }

    @Bean
    public DefaultKafkaProducerFactory<String, OrderCreatedevent>
    orderProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers
    ) {
        return new DefaultKafkaProducerFactory<>(
                producerProperties(servers),
                new StringSerializer(),
                new JacksonJsonSerializer<OrderCreatedevent>()
        );
    }

    @Bean("orderKafkaTemplate")
    public KafkaTemplate<String, OrderCreatedevent> orderKafkaTemplate(
            @Qualifier("orderProducerFactory")
            DefaultKafkaProducerFactory<String, OrderCreatedevent> factory
    ) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public DefaultKafkaProducerFactory<String, String> dltProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers
    ) {
        return new DefaultKafkaProducerFactory<>(
                producerProperties(servers),
                new StringSerializer(),
                new StringSerializer()
        );
    }

    @Bean("dltKafkaTemplate")
    public KafkaTemplate<String, String> dltKafkaTemplate(
            @Qualifier("dltProducerFactory")
            DefaultKafkaProducerFactory<String, String> factory
    ) {
        return new KafkaTemplate<>(factory);
    }
}
