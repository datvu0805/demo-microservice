package dat.com.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("inventory-service", r -> r
                        .path("/api/inventory", "/api/inventory/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://inventory-service")
                )
                .route("order-service", r -> r
                        .path("/api/orders", "/api/orders/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service")
                )
                .build();
    }
}
