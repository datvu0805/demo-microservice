package dat.com.orderservice.infrastructure.persistence.entity;

public enum OrderOutboxStatus {

    NEW,

    PROCESSING,

    PUBLISHED
}