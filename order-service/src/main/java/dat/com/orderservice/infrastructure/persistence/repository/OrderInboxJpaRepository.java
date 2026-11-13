package dat.com.orderservice.infrastructure.persistence.repository;

import dat.com.orderservice.infrastructure.persistence.entity.OrderInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderInboxJpaRepository extends JpaRepository<OrderInboxEntity, UUID> {

    //insert ignore ết hợp uniquekey nếu khóa chưa có thì insert , ếu đã tồn tại k cập nhật nữa, cũng k ném exception
    @Modifying
    @Query(value = """
    INSERT IGNORE INTO order_event_inbox
        (event_id, order_id, topic, processed_at)
    VALUES (:eventId, :orderId, :topic, CURRENT_TIMESTAMP)
    """, nativeQuery = true)
    int claimEvent(
            @Param("eventId") String eventId,
            @Param("orderId") Long orderId,
            @Param("topic") String topic
    );
}
