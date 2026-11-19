package dat.com.orderservice.infrastructure.scheduling;

import dat.com.orderservice.application.command.ReleaseStockCommand;
import dat.com.orderservice.application.command.ReserveStockCommand;
import dat.com.orderservice.application.saga.OrderSagaState;
import dat.com.orderservice.config.KafkaTopicConfig;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderOutboxEntity;
import dat.com.orderservice.infrastructure.persistence.entity.OrderSagaEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderOutboxJpaRepository;
import dat.com.orderservice.infrastructure.persistence.repository.OrderSagaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaRecovery {

    private final OrderSagaJpaRepository sagaRepository;
    private final OrderOutboxJpaRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${order.saga.recovery-retry-delay:PT30S}")
    private Duration retryDelay;

    @Scheduled(fixedDelayString = "${order.saga.recovery-scan-delay-ms:10000}")
    @Transactional
    public void recoverExpiredSagas() {
        var due = sagaRepository.findAllByStateInAndDeadlineAtBefore(
                List.of(OrderSagaState.WAITING_STOCK, OrderSagaState.WAITING_RELEASE),
                Instant.now(),
                PageRequest.of(0, 100)
        );

        for (OrderSagaEntity saga : due) {
            Order order = orderRepository.findById(saga.getOrderId()).orElse(null);
            if (order == null) {
                log.error("Cannot recover Saga without Order: sagaId={}, orderId={}",
                        saga.getId(), saga.getOrderId());
                continue;
            }

            String topic;
            String payload;
            if (saga.getState() == OrderSagaState.WAITING_STOCK) {
                ReserveStockCommand command = new ReserveStockCommand(
                        saga.getCurrentCommandId(), saga.getId(), saga.getOrderId(),
                        order.getProductId(), order.getQuantity()
                );
                topic = KafkaTopicConfig.INVENTORY_RESERVE_REQUESTED;
                payload = objectMapper.writeValueAsString(command);
            } else {
                ReleaseStockCommand command = new ReleaseStockCommand(
                        saga.getCurrentCommandId(), saga.getId(), saga.getOrderId(),
                        order.getProductId(), order.getQuantity()
                );
                topic = KafkaTopicConfig.INVENTORY_RELEASE_COMMAND;
                payload = objectMapper.writeValueAsString(command);
            }

            ensureCommandQueued(
                    saga.getCurrentCommandId(),
                    saga.getOrderId(),
                    topic,
                    payload
            );
            saga.deferRecovery(retryDelay);
            log.warn("Saga command recovery queued: sagaId={}, state={}, commandId={}",
                    saga.getId(), saga.getState(), saga.getCurrentCommandId());
        }
    }

    private void ensureCommandQueued(
            UUID commandId,
            UUID orderId,
            String topic,
            String payload
    ) {
        List<OutboxRow> rows = jdbcTemplate.query("""
                SELECT id, status
                FROM order_outbox
                WHERE event_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new OutboxRow(
                        rs.getLong("id"), rs.getString("status")
                ),
                commandId.toString()
        );

        if (rows.isEmpty()) {
            outboxRepository.save(OrderOutboxEntity.create(
                    commandId, orderId, topic, payload
            ));
            return;
        }

        OutboxRow row = rows.getFirst();
        switch (row.status()) {
            case "NEW", "PROCESSING" -> { }
            case "PUBLISHED" -> {
                int updated = jdbcTemplate.update("""
                        UPDATE order_outbox
                        SET status = 'NEW', available_at = CURRENT_TIMESTAMP(6),
                            published_at = NULL, locked_until = NULL, claim_token = NULL
                        WHERE id = ? AND status = 'PUBLISHED'
                        """, row.id());
                if (updated != 1) {
                    throw new IllegalStateException(
                            "Không thể xếp lại command vào Outbox: " + commandId
                    );
                }
            }
            default -> throw new IllegalStateException(
                    "Trạng thái Outbox không hỗ trợ: " + row.status()
            );
        }
    }

    private record OutboxRow(Long id, String status) { }
}
