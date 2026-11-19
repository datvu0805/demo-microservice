package dat.com.orderservice.application.saga;

public enum OrderSagaState {

    WAITING_STOCK,

    WAITING_RELEASE,

    COMPLETED,

    REJECTED,

    COMPENSATED;

    public boolean isTerminal() {
        return this == COMPLETED
                || this == REJECTED
                || this == COMPENSATED;
    }
}