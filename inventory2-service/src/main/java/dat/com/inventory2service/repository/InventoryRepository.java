package dat.com.inventory2service.repository;

import dat.com.inventory2service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    Optional<Inventory> findByProductId(Long productId);
}
