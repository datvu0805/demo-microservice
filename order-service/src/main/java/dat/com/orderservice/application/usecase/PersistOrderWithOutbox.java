package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.event.OrderCreatedevent;
import dat.com.orderservice.application.idempotency.OrderRequestClaim;
import dat.com.orderservice.config.KafkaTopicConfig;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderOutboxJpaRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderRequestClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistOrderWithOutbox {

    private final OrderRepository orderRepository;
    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final ObjectMapper objectMapper;
    private final OrderRequestClaimRepository orderRequestClaimRepository;

    @Transactional
    public Order execute(Order order,String idempotencyKey,String payloadHash){

        //claim quyền tạo đơn
        OrderRequestClaim claim = orderRequestClaimRepository.claim(idempotencyKey, payloadHash);

        //check cùng key nhưng khac nội dung-> từ chối
        if(!payloadHash.equals(claim.payloadHash())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"idempotenceKey đã được dùng cho nội dung khác");
        }

        //key trùng , payload cx trùng -> trả bản cũ
        if (!claim.owned()){

            if(claim.orderId() == null){
                throw new IllegalArgumentException("Bản ghi đã tồn tại nhưng thiếu orderId");
            }

            return orderRepository.findById(claim.orderId())
                    .orElseThrow(()-> new IllegalArgumentException("Không tìm thấy order của idempotencyKey"));
        }



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

        orderRequestClaimRepository.complete(idempotencyKey, claim.claimToken(), savedOrder.getId());

        return savedOrder;
    }
}
