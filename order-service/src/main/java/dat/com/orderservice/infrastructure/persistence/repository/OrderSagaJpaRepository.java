package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.application.saga.OrderSagaType;
import dat.com.orderservice.infrastructure.persistence.entity.OrderSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface OrderSagaJpaRepository
        extends JpaRepository<OrderSagaEntity, UUID> {

    Optional<OrderSagaEntity> findByOrderIdAndType(
            UUID orderId,
            OrderSagaType type
    );

    List<OrderSagaEntity> findAllByOrderId(UUID orderId);

    Optional<OrderSagaEntity> findByReleaseEventId(UUID releaseEventId);
}
