package dat.com.inventory2service.service;

import com.thoughtworks.xstream.mapper.Mapper;
import dat.com.inventory2service.entity.Inventory;
import dat.com.inventory2service.grpc.CheckStockRequest;
import dat.com.inventory2service.grpc.CheckStockResponse;
import dat.com.inventory2service.grpc.InventoryServiceGrpc;
import dat.com.inventory2service.repository.InventoryRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    public void checkStock(CheckStockRequest request,
                           StreamObserver<CheckStockResponse> responseStreamObserver){

        //1. lấy dữ liệu từ request grpc
        long productId = request.getProductId();
        int quantity = request.getQuantity();

        //2.validate productId và số lượng xem có hợp lệ k
        if(productId <=0 || quantity<=0){
            responseStreamObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(" id product hoặc quantity không hợp lệ")
                    .asRuntimeException());
            return;
        }
        //3. tìm tồn kho
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(null);

        if(inventory == null){
            responseStreamObserver.onError(Status.NOT_FOUND
                    .withDescription("Không có trong kho")
                    .asRuntimeException());
            return;
        }

        //4.Kiểm tra số lượng hàng
        int currentQuantity = inventory.getQuantity()- inventory.getReservedQuantity();

        boolean available = currentQuantity>=quantity;

        //5. tạo response grpc
        CheckStockResponse response = CheckStockResponse.newBuilder()
                .setAvailable(available)
                .setCurrentQuantity(currentQuantity)
                .build();

        //6.trả dữ liệu  cho client
        responseStreamObserver.onNext(response);
        responseStreamObserver.onCompleted();
    }
}
