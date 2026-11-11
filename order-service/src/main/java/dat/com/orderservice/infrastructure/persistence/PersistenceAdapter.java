package dat.com.orderservice.infrastructure.persistence;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import dat.com.orderservice.infrastructure.persistence.mapper.OrderPersistenceMapper;
import dat.com.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.Objects;
import java.util.Optional;
@Repository
@RequiredArgsConstructor
public class PersistenceAdapter implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper mapper;

    @Transactional
    @Override
    public Order save(Order order) {

        if(order.getId()==null){
            OrderJpaEntity newEntity = mapper.toEntity(order);
            OrderJpaEntity saved =orderJpaRepository.saveAndFlush(newEntity);
            return mapper.toDomain(saved);
        }

        OrderJpaEntity entity = orderJpaRepository.findById(order.getId())
                .orElseThrow(()
                        -> new IllegalArgumentException("Khng có id"));

        if(!Objects.equals(entity.getVersion(),order.getVersion())){
            throw new ObjectOptimisticLockingFailureException(OrderJpaEntity.class,order.getId());
        }

        entity.setStatus(order.getStatus().name());

        orderJpaRepository.flush();

        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id).map(mapper::toDomain);
    }
}
