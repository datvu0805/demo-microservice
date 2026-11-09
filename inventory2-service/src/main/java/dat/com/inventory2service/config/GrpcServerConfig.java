package dat.com.inventory2service.config;


import dat.com.inventory2service.service.InventoryGrpcService;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {


    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server grpcServer(InventoryGrpcService inventoryGrpcService) {

        return NettyServerBuilder
                .forPort(9090)
                .addService(inventoryGrpcService)
                .build();
    }
}
