package dat.com.orderservice.infrastructure.scheduling;

import dat.com.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaWatchdog {

    private final OrderJpaRepository orderJpaRepository;

    @Value("${order.saga.stuck-after:PT2M}")
    private Duration stuckAfter;

    @Scheduled(
            fixedDelayString = "${order.saga.scan-delay-ms:10000}"
    )
    public void scan() {

        Instant now = Instant.now();
        Instant cutoff = now.minus(stuckAfter);

        var candidates = orderJpaRepository.findStuckCandidates(
                cutoff,
                PageRequest.of(0, 100)
        );

        for (var order : candidates) {
            long waitingSeconds = Duration.between(
                    order.getStatusChangedAt(),
                    now
            ).toSeconds();

            log.warn(
                    "Saga waiting too long: orderId={}, status={}, "
                            + "waitingSeconds={}, version={}",
                    order.getId(),
                    order.getStatus(),
                    waitingSeconds,
                    order.getVersion()
            );
        }
    }
}