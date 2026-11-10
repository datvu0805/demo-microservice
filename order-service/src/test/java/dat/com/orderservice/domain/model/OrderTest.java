package dat.com.orderservice.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCreatePendingOrder() {

        Order order = Order.create(
                1L,
                "Laptop",
                new BigDecimal("15000000"),
                2
        );

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(
                0,
                new BigDecimal("30000000")
                        .compareTo(order.getTotalAmount())
        );
    }

    @Test
    void shouldNotCancelConfirmedOrder() {

        Order order = Order.create(
                1L,
                "Laptop",
                new BigDecimal("15000000"),
                2
        );

        order.confirm();

        assertThrows(
                IllegalStateException.class,
                order::cancel
        );
    }
}