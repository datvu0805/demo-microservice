package dat.com.orderservice.client;

import dat.com.orderservice.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Qualifier("loadBalancedRestClientBuilder")
            RestClient.Builder builder) {

        this.restClient = builder
                .baseUrl("http://product-service")
                .build();
    }

    public ProductResponse getProduct(Long productId) {

        return restClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .body(ProductResponse.class);
    }
}