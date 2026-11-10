package dat.com.orderservice.infrastructure.persistence;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import dat.com.orderservice.infrastructure.persistence.mapper.OrderPersistenceMapper;
import dat.com.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
@RequiredArgsConstructor
public class PersistenceAdapter implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper mapper;

    @Transactional
    @Override
    public Order save(Order order) {
        OrderJpaEntity entity=mapper.toEntity(order);
        OrderJpaEntity savedEntity = orderJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id).map(mapper::toDomain);
    }
}
