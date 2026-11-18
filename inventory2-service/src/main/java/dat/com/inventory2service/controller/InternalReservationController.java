package dat.com.inventory2service.controller;

import dat.com.inventory2service.dto.ReservationSnapshotResponse;
import dat.com.inventory2service.service.GetReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventory/reservations")
public class InternalReservationController {

    private final GetReservationService getReservationService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ReservationSnapshotResponse> get(
            @PathVariable UUID orderId
    ) {
        return getReservationService
                .execute(orderId)
                .map(snapshot ->
                        ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(snapshot)
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }
}