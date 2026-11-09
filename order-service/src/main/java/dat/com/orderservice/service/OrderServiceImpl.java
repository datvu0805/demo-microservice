package dat.com.orderservice.service;

import dat.com.orderservice.client.ProductClient;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.dto.ProductResponse;
import dat.com.orderservice.entity.Order;
import dat.com.orderservice.entity.OrderStatus;
import dat.com.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    @Override
    @Transactional
    public Order create(CreateOrderRequest request) {

        log.info("Product ID: {}", request.productId());
        log.info("Product Name: {}", request.productName());
        log.info("Quantity: {}", request.quantity());
        log.info("Price: {}", request.price());

        ProductResponse product = productClient.getProduct(request.productId());

        Order order = Order.builder()
                .productName(product.name())
                .productId(product.id())
                .quantity(product.stockQuantity())
                .price(product.price())
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(order);
        return new Order(
                saved.getId(),
                saved.getProductId(),
                saved.getProductName(),
                saved.getPrice(),
                saved.getQuantity(),
                saved.getStatus()
        );
    }
}
