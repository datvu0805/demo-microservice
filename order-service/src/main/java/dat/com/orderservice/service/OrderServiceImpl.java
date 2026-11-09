package dat.com.orderservice.service;

import dat.com.orderservice.client.InventoryGrpcClient;
import dat.com.orderservice.client.ProductClient;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.dto.ProductResponse;
import dat.com.orderservice.entity.Order;
import dat.com.orderservice.entity.OrderStatus;
import dat.com.orderservice.grpc.CheckStockResponse;
import dat.com.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryGrpcClient inventoryGrpcClient;

    @Override
    @Transactional
    public Order create(CreateOrderRequest request) {


        log.info("Product ID: {}", request.productId());
        log.info("Product Name: {}", request.productName());
        log.info("Quantity: {}", request.quantity());
        log.info("Price: {}", request.price());

        //1.Lấy thông tin product qua rest
        ProductResponse product = productClient.getProduct(request.productId());

        //2.check stock qua grpc
        CheckStockResponse stockResponse = inventoryGrpcClient.checkStock(request.productId(),request.quantity());


        //3.check số lượng hàng
        if(!stockResponse.getAvailable()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Không đủ hàng");
        }

        //4.Tạo order
        Order order = Order.builder()
                .productName(product.name())
                .productId(product.id())
                .quantity(product.stockQuantity())
                .price(product.price())
                .status(OrderStatus.PENDING)
                .build();
        //5. lưu vào db
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
