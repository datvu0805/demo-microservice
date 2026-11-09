package dat.com.orderservice.controller;

import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.entity.Order;
import dat.com.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public Order create(@Valid @RequestBody CreateOrderRequest request){
        return orderService.create(request)  ;
    }
}
