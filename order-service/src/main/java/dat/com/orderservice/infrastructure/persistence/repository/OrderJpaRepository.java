package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity,Long> {
}
