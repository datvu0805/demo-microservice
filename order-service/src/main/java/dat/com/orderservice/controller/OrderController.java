package dat.com.orderservice.controller;

import dat.com.orderservice.application.usecase.CancelOrderUseCase;
import dat.com.orderservice.application.usecase.ConfirmUseCase;
import dat.com.orderservice.application.usecase.CreateOrderUseCase;
import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderUseCase create;
    private final CancelOrderUseCase cancel;
    private final ConfirmUseCase confirm;
    private final OrderJpaRepository orderJpaRepository;

    @PostMapping
    public Order create(@Valid @RequestBody CreateOrderRequest request,@RequestHeader("Idempotency-Key")String idempotenceKey){
        return create.execute(request,idempotenceKey)  ;
    }


    @GetMapping("/all")
    public List<OrderJpaEntity> listOrder(){
        return orderJpaRepository.findAll();
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
