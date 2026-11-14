package dat.com.orderservice.application.usecase;

import dat.com.orderservice.client.InventoryGrpcClient;
import dat.com.orderservice.client.ProductClient;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.dto.ProductResponse;
import dat.com.orderservice.grpc.CheckStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final ProductClient productClient;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final PersistOrderWithOutbox persistOrderWithOutbox;

    public Order execute(CreateOrderRequest request){
        //1. Lấy thông tin sản phẩn qua rest
       ProductResponse productResponse= productClient.getProduct(request.productId());
            if(productResponse == null || productResponse.id() == null){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Product service trả về dữ liệu rỗng");
            }

        //2.ktra kho qua grpc
        CheckStockResponse checkStockResponse = inventoryGrpcClient.checkStock(request.productId(),request.quantity());

        //3. validate available

        if(!checkStockResponse.getAvailable()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Tồn kho không khả dụng");
        }

        //4.Tạo domain order

        Order order = Order.create(
                productResponse.id(),
                productResponse.name(),
                productResponse.price(),
                request.quantity()
                );
        return persistOrderWithOutbox.execute(order);
    }
}
