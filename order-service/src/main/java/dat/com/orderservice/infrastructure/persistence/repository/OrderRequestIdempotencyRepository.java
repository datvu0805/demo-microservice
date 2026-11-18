package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.infrastructure.persistence.entity.OrderRequestIdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRequestIdempotencyRepository extends JpaRepository<OrderRequestIdempotencyEntity,String> {
}
