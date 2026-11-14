package dat.com.orderservice.application.usecase;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional
    public Order execute(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));

        order.cancel();

        return orderRepository.save(order);
    }
}
