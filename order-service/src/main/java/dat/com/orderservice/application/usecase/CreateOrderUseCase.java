package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.idempotency.OrderRequestHasher;
import dat.com.orderservice.client.ProductClient;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.dto.ProductResponse;
import dat.com.orderservice.infrastructure.persistence.repository.OrderRequestIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private static final Pattern KEY_PATTERN =
            Pattern.compile("[a-z0-9_-]{1,64}");

    private final ProductClient productClient;
    private final OrderRepository orderRepository;
    private final PersistOrderWithOutbox persistOrderWithOutbox;
    private final OrderRequestHasher requestHasher;
    private final OrderRequestIdempotencyRepository idempotencyRepository;


    public Order execute(CreateOrderRequest request, String idempotencyKey) {

        //ktra key và request
        validate(idempotencyKey, request);

        //Tính hash từ nội dung yêu cầu
        String payloadHash = requestHasher.hash(request.productId(), request.quantity());

        //request đã hoàn tất thì trả Order  cũ
        Optional<Order> existingOrder = findExistingOrder(idempotencyKey,payloadHash);

        if(existingOrder.isPresent()){
            return existingOrder.get();
        }



        //1. Lấy thông tin sản phẩn qua rest
        ProductResponse productResponse = productClient.getProduct(request.productId());
        if (productResponse == null || productResponse.id() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product service trả về dữ liệu rỗng");
        }

        // Persist the order first. Inventory reservation runs asynchronously from order.created.
        Order order = Order.create(
                productResponse.id(),
                productResponse.name(),
                productResponse.price(),
                request.quantity()
        );

        return persistOrderWithOutbox.execute(order, idempotencyKey,payloadHash);
    }

    private void validate(
            String idempotencyKey,
            CreateOrderRequest request
    ) {

        if (idempotencyKey == null
                || !KEY_PATTERN.matcher(idempotencyKey).matches()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key phải dài 1–64 ký tự, "
                            + "chỉ gồm a-z, 0-9, dấu - và _"
            );
        }

        if (request == null
                || request.productId() == null
                || request.productId() <= 0
                || request.quantity() == null
                || request.quantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "productId và quantity phải lớn hơn 0"
            );
        }

    }
    private Optional<Order> findExistingOrder(
            String idempotencyKey,
            String payloadHash
    ) {

        return idempotencyRepository.findById(idempotencyKey)
                .map(existing -> {

                    if (!payloadHash.equals(existing.getPayloadHash())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Idempotency-Key đã được dùng cho nội dung khác"
                        );
                    }

                    if (existing.getOrderId() == null) {
                        throw new IllegalStateException(
                                "Bản ghi idempotency thiếu Order ID"
                        );
                    }

                    return orderRepository.findById(
                            existing.getOrderId()
                    ).orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy Order của idempotency key"
                    ));
                });
    }

}
