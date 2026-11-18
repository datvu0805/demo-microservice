package dat.com.orderservice.application.usecase;

import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.dto.ReleaseStockCommand;
import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private static final String RELEASE_REQUESTED =
            "inventory.release.requested";

    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @Transactional
    public Order execute(UUID orderId) {

        //1đọc order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));

        //2.order quết  định có cần chuyển trạng thai k
        boolean changed = order.requestCancellation();

        //đã xaác định cancelling hoặc canceled thì k tạo thêm command
        if(!changed){
            return order;
        }

        //3.lưu cancelling, ktra version
        Order saved = orderRepository.save(order);

        //4Tạo event yêu cầu giải phóng kho

        UUID eventId = UUID.randomUUID();
        ReleaseStockCommand command = new ReleaseStockCommand(eventId,saved.getId(), saved.getProductId(), saved.getQuantity());

        //5.Lưu command vào outbox
        orderOutboxJpaRepository.save(OrderOutboxEntity.create(eventId,saved.getId(),RELEASE_REQUESTED,objectMapper.writeValueAsString(command)));


        return saved;
    }
}
