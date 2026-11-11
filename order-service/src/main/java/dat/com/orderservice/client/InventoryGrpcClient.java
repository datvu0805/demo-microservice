package dat.com.orderservice.client;

import dat.com.orderservice.grpc.CheckStockRequest;
import dat.com.orderservice.grpc.CheckStockResponse;
import dat.com.orderservice.grpc.InventoryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InventoryGrpcClient {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public InventoryGrpcClient(ManagedChannel inventoryChannel) {
        this.stub = InventoryServiceGrpc.newBlockingStub(inventoryChannel);
    }

    public CheckStockResponse checkStock(Long productId, Integer quantity) {

        CheckStockRequest request = CheckStockRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(quantity)
                .build();

        try {
            log.info("Calling Inventory gRPC: productId={}, quantity={}",
                    productId, quantity);

            return stub
                    .withDeadlineAfter(100, TimeUnit.SECONDS)
                    .checkStock(request);

        } catch (StatusRuntimeException ex) {

            log.error(
                    "gRPC failed: code={}, description={}, cause={}",
                    ex.getStatus().getCode(),
                    ex.getStatus().getDescription(),
                    ex.getCause() == null
                            ? "none"
                            : ex.getCause().toString(),
                    ex
            );

            Status.Code code = ex.getStatus().getCode();

            if (code == Status.Code.NOT_FOUND) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found",
                        ex
                );
            }

            if (code == Status.Code.INVALID_ARGUMENT) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid inventory request",
                        ex
                );
            }

            if (code == Status.Code.DEADLINE_EXCEEDED) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Inventory Service timeout",
                        ex
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Inventory Service unavailable",
                    ex
            );
        }
    }
}