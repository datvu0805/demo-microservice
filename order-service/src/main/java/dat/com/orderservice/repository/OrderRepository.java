package dat.com.orderservice.repository;

import dat.com.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long> {
}
