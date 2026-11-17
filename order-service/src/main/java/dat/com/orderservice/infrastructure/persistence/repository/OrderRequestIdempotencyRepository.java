package dat.com.orderservice.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRequestIdempotencyRepository extends JpaRepository<OrderRequestIdempotencyRepository,String> {
}
