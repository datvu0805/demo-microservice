package dat.com.inventory2service.repository;

import dat.com.inventory2service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    Optional<Inventory> findByProductId(Long productId);

    //Atomic codittion update
    @Modifying
    @Query(value = """
    UPDATE inventory
    SET reserved_quantity = reserved_quantity + :quantity
    WHERE product_id = :productId
      AND quantity - reserved_quantity >= :quantity
    """, nativeQuery = true)
    int reserveStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}
