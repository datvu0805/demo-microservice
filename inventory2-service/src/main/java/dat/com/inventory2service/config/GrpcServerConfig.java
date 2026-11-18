package dat.com.inventory2service.config;


import dat.com.inventory2service.service.InventoryGrpcService;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class GrpcServerConfig {


    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server grpcServer(InventoryGrpcService inventoryGrpcService,
                             @Value("${inventory.grpc.port:9090}") int port) {

        Server server = NettyServerBuilder
                .forPort(port)
                .addService(inventoryGrpcService)
                .build();
        log.info("Starting Inventory gRPC server on port {}", port);
        return server;
    }
}
