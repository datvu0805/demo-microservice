package dat.com.orderservice.infrastructure.persistence.entity;

import dat.com.orderservice.application.saga.OrderSagaState;
import dat.com.orderservice.application.saga.OrderSagaType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "order_sagas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_saga_order_type",
                        columnNames = {"order_id", "saga_type"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_saga_state_deadline",
                        columnList = "state,deadline_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSagaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "reserve_command_id",
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID reserveCommandId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "release_event_id",
            unique = true,
            columnDefinition = "BINARY(16)"
    )
    private UUID releaseEventId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "order_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "saga_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private OrderSagaType type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "state",
            nullable = false,
            length = 30
    )
    private OrderSagaState state;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "current_command_id",
            nullable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID currentCommandId;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static OrderSagaEntity startCreateOrder(
            UUID orderId,
            Duration stockTimeout
    ) {
        Objects.requireNonNull(orderId, "orderId không được null");
        Objects.requireNonNull(
                stockTimeout,
                "stockTimeout không được null"
        );

        if (stockTimeout.isZero() || stockTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "stockTimeout phải lớn hơn 0"
            );
        }

        Instant now = Instant.now();

        OrderSagaEntity saga = new OrderSagaEntity();

        saga.id = UUID.randomUUID();
        saga.orderId = orderId;
        saga.type = OrderSagaType.CREATE_ORDER;
        saga.state = OrderSagaState.WAITING_STOCK;

        saga.currentCommandId = UUID.randomUUID();
        // Giữ lại correlation id của reserve kể cả khi currentCommandId
        // được thay bằng release command lúc bắt đầu bù trừ.
        saga.reserveCommandId = saga.currentCommandId;

        saga.deadlineAt = now.plus(stockTimeout);
        saga.createdAt = now;
        saga.updatedAt = now;

        return saga;
    }

    public void completeStockReservation() {
        requireWaitingStock();

        finish(OrderSagaState.COMPLETED);
    }

    public void rejectStockReservation() {
        requireWaitingStock();

        finish(OrderSagaState.REJECTED);
    }

    public void completeRelease(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId không được null");

        if (state == OrderSagaState.COMPENSATED) {
            if (eventId.equals(releaseEventId)) {
                return;
            }
            throw new IllegalStateException(
                    "Saga đã được bù trừ bởi release event khác"
            );
        }

        if (state != OrderSagaState.WAITING_RELEASE) {
            throw new IllegalStateException(
                    "Saga không chờ kết quả release: " + state
            );
        }

        this.releaseEventId = eventId;
        finish(OrderSagaState.COMPENSATED);
    }

    public void deferRecovery(Duration retryDelay) {
        requirePositiveTimeout(retryDelay);
        if (state != OrderSagaState.WAITING_STOCK
                && state != OrderSagaState.WAITING_RELEASE) {
            throw new IllegalStateException(
                    "Chỉ Saga đang chờ mới được lên lịch recovery"
            );
        }
        Instant now = Instant.now();
        this.deadlineAt = now.plus(retryDelay);
        this.updatedAt = now;
    }

    private void requireWaitingStock() {
        if (type != OrderSagaType.CREATE_ORDER) {
            throw new IllegalStateException(
                    "Chỉ CREATE_ORDER Saga mới xử lý kết quả reserve"
            );
        }

        if (state != OrderSagaState.WAITING_STOCK) {
            throw new IllegalStateException(
                    "Saga không chờ reserve: " + state
            );
        }
    }

    private void finish(OrderSagaState finalState) {
        Instant now = Instant.now();

        this.state = finalState;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void requestRelease(Duration releaseTimeout) {
        requirePositiveTimeout(releaseTimeout);

        if (type != OrderSagaType.CREATE_ORDER
                || state != OrderSagaState.WAITING_STOCK) {
            throw new IllegalStateException(
                    "Chỉ Saga CREATE_ORDER đang WAITING_STOCK "
                            + "mới được bắt đầu bù trừ"
            );
        }

        Instant now = Instant.now();

        this.state = OrderSagaState.WAITING_RELEASE;
        this.currentCommandId = UUID.randomUUID();
        this.deadlineAt = now.plus(releaseTimeout);
        this.updatedAt = now;
    }

    public static OrderSagaEntity startCancelOrder(
            UUID orderId,
            Duration releaseTimeout
    ) {
        Objects.requireNonNull(orderId, "orderId không được null");
        requirePositiveTimeout(releaseTimeout);

        Instant now = Instant.now();

        OrderSagaEntity saga = new OrderSagaEntity();

        saga.id = UUID.randomUUID();
        saga.orderId = orderId;
        saga.type = OrderSagaType.CANCEL_ORDER;
        saga.state = OrderSagaState.WAITING_RELEASE;

        saga.currentCommandId = UUID.randomUUID();

        saga.deadlineAt = now.plus(releaseTimeout);
        saga.createdAt = now;
        saga.updatedAt = now;

        return saga;
    }

    private static void requirePositiveTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout không được null");

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout phải lớn hơn 0"
            );
        }
    }
}
