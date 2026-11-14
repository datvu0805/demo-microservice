package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, Long> {
}
