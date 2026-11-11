package dat.com.inventory2service.repository;

import dat.com.inventory2service.entity.InventoryOutboxEntity;
import dat.com.inventory2service.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryOutboxJpaRepository extends JpaRepository<InventoryOutboxEntity,Long> {
    List<InventoryOutboxEntity> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
}
