package dat.com.inventory2service.service;

import dat.com.inventory2service.config.InventorySagaTopicConfig;
import dat.com.inventory2service.dto.ReserveStockResult;
import dat.com.inventory2service.dto.ReleaseStockResult;
import dat.com.inventory2service.entity.Inventory;
import dat.com.inventory2service.entity.InventoryOutboxEntity;
import dat.com.inventory2service.repository.InventoryOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPersistenceService {

    private final InventoryOutboxJpaRepository inventoryOutboxJpaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveStockResult(UUID orderId,Long productId,int quantity,String topic){
        try {
            UUID eventId = UUID.randomUUID();
            Map<String, Object> event = Map.of(
                    "eventId",eventId.toString(),
                    "orderId",orderId.toString(),
                    "productId",productId,
                    "quantity",quantity
            );

            String payload = objectMapper.writeValueAsString(event);

            InventoryOutboxEntity outbox = InventoryOutboxEntity.create(
                   eventId,orderId,topic,payload
            );
            inventoryOutboxJpaRepository.save(outbox);
        }catch (Exception ex){
            throw new IllegalArgumentException("Tạo outbox thất bại",ex);
        }
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public void saveReserveResult(ReserveStockResult result) {

        if (result == null
                || result.eventId() == null
                || result.sagaId() == null
                || result.commandId() == null
                || result.orderId() == null
                || result.productId() == null
                || result.productId() <= 0
                || result.quantity() <= 0
                || result.outcome() == null) {

            throw new IllegalArgumentException(
                    "ReserveStockResult không hợp lệ"
            );
        }

        String payload = objectMapper.writeValueAsString(result);

        InventoryOutboxEntity outbox = InventoryOutboxEntity.create(
                result.eventId(),
                result.orderId(),
                InventorySagaTopicConfig.RESERVE_RESULT,
                payload
        );

        inventoryOutboxJpaRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveReleaseResult(ReleaseStockResult result) {
        if (result == null
                || result.eventId() == null
                || result.sagaId() == null
                || result.commandId() == null
                || result.orderId() == null
                || result.productId() == null
                || result.productId() <= 0
                || result.quantity() <= 0
                || result.outcome() == null) {
            throw new IllegalArgumentException("ReleaseStockResult không hợp lệ");
        }

        String payload = objectMapper.writeValueAsString(result);
        inventoryOutboxJpaRepository.save(InventoryOutboxEntity.create(
                result.eventId(),
                result.orderId(),
                InventorySagaTopicConfig.RELEASE_RESULT,
                payload
        ));
    }
}
