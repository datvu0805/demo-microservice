package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    @Query("""
        SELECT o
        FROM OrderJpaEntity o
        WHERE o.status IN ('PENDING', 'CANCELLING')
          AND o.statusChangedAt < :cutoff
        ORDER BY o.statusChangedAt ASC, o.id ASC
        """)
    List<OrderJpaEntity> findStuckCandidates(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

}
