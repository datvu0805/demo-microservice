package dat.com.orderservice.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel inventoryChannel(
            @Value("${inventory.grpc.host:127.0.0.1}") String host,
            @Value("${inventory.grpc.port:9090}") int port
    ) {

        log.info("Creating Inventory gRPC channel: {}:{}", host, port);
        log.info("Inventory gRPC HOST={}, PORT={}", host, port);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        log.info("Inventory gRPC authority: {}", channel.authority());

        return channel;
    }
}