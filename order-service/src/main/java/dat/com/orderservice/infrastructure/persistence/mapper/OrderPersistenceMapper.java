package dat.com.orderservice.infrastructure.persistence.mapper;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.model.OrderStatus;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toEntity(Order order){

        OrderJpaEntity entity = new OrderJpaEntity();

        entity.setId(order.getId());
        entity.setPrice(order.getPrice());
        entity.setProductName(order.getProductName());
        entity.setProductId(order.getProductId());
        entity.setQuantity(order.getQuantity());
        entity.setStatus(order.getStatus().name());

        return entity;
    }


    public Order toDomain(OrderJpaEntity entity) {

        return Order.rehydrate(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getPrice(),
                entity.getQuantity(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getVersion()
        );

    }
}
