package dat.com.inventory2service.service;

import dat.com.inventory2service.dto.ReservationSnapshotResponse;
import dat.com.inventory2service.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetReservationService{

    private final InventoryReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public Optional<ReservationSnapshotResponse> execute(UUID orderId) {

        return reservationRepository.findByOrderId(orderId)
                .map(reservation -> new ReservationSnapshotResponse(
                        reservation.getOrderId(),
                        reservation.getProductId(),
                        reservation.getQuantity(),
                        reservation.getStatus()
                ));
    }
}