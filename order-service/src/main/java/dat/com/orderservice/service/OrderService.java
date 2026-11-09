package dat.com.orderservice.service;

import dat.com.orderservice.dto.CreateOrderRequest;
import dat.com.orderservice.entity.Order;

public interface OrderService {
    Order create(CreateOrderRequest request);
}
