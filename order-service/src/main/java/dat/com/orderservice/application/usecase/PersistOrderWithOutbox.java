package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.event.OrderCreatedevent;
import dat.com.orderservice.config.KafkaTopicConfig;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistOrderWithOutbox {

    private final OrderRepository orderRepository;
    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order execute(Order order){

        Order savedOrder = orderRepository.save(order);

        UUID eventId = UUID.randomUUID();

        OrderCreatedevent event = new OrderCreatedevent(
                eventId,
                savedOrder.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getPrice()
                );
        //chuyển event thành json
        String payload = objectMapper.writeValueAsString(event);

        OrderOutboxEntity outbox = OrderOutboxEntity.create(eventId, savedOrder.getId(), KafkaTopicConfig.ORDER_CREATED,payload);

        orderOutboxJpaRepository.save(outbox);

        return savedOrder;
    }
}
