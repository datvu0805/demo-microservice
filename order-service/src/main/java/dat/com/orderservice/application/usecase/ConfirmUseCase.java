package dat.com.orderservice.application.usecase;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ConfirmUseCase {

    private final OrderRepository orderRepository;

    public Order execute(Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow(()->new IllegalArgumentException("Đơn hàng k có"));

        order.confirm();

        return orderRepository.save(order);
    }
}
