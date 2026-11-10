package dat.com.orderservice.application.usecase;

import dat.com.orderservice.client.InventoryGrpcClient;
import dat.com.orderservice.client.ProductClient;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.dto.ProductResponse;
import dat.com.orderservice.grpc.CheckStockResponse;
import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryGrpcClient inventoryGrpcClient;

    public Order execute(CreateOrderRequest request){
        //1. Lấy thông tin sản phẩn qua rest
       ProductResponse productResponse= productClient.getProduct(request.productId());

        //2.ktra kho qua grpc
        CheckStockResponse checkStockResponse = inventoryGrpcClient.checkStock(request.productId(),request.quantity());

        //3. validate available

        if(!checkStockResponse.getAvailable()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Tồn kho không khả dụng");
        }

        //4.Tạo domain order
        Order order = Order.create(request.productId(),
                request.productName(),
                request.price(),
                request.quantity());
        //5.Lưu order qua domain repository
        return orderRepository.save(order);
    }
}
