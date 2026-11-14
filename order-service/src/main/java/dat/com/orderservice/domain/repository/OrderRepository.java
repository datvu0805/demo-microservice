package dat.com.orderservice.domain.repository;

import dat.com.orderservice.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID id);

}
