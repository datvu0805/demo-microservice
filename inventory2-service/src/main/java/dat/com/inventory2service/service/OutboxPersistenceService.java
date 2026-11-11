package dat.com.inventory2service.service;

import dat.com.inventory2service.entity.Inventory;
import dat.com.inventory2service.entity.InventoryOutboxEntity;
import dat.com.inventory2service.repository.InventoryOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    public void saveStockResult(Long orderId,Long productId,int quantity,String topic){
        try {
            UUID eventId = UUID.randomUUID();
            Map<String, Object> event = Map.of(
                    "eventId",eventId.toString(),
                    "orderId",orderId,
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
}
