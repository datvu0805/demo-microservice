package dat.com.orderservice.controller;

import dat.com.orderservice.application.usecase.CancelOrderUseCase;
import dat.com.orderservice.application.usecase.ConfirmUseCase;
import dat.com.orderservice.application.usecase.CreateOrderUseCase;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.domain.model.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderUseCase create;
    private final CancelOrderUseCase cancel;
    private final ConfirmUseCase confirm;

    @PostMapping
    public Order create(@Valid @RequestBody CreateOrderRequest request){
        return create.execute(request)  ;
    }

    @PostMapping("/{id}/confirm")
    public Order confrim(@PathVariable UUID id){
        Order order = confirm.execute(id);
        return new
                Order(order.getId(),order.getProductId(),
                order.getProductName(),order.getPrice(),
                order.getQuantity(),order.getStatus(),order.getVersion());
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable UUID id){
        Order order = cancel.execute(id);
        return new
                Order(order.getId(),order.getProductId(),
                order.getProductName(),order.getPrice(),
                order.getQuantity(),order.getStatus(), order.getVersion());
    }

}
