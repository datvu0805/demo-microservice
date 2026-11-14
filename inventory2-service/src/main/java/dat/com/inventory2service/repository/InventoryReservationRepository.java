package dat.com.inventory2service.repository;

import dat.com.inventory2service.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation,Long> {
    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
