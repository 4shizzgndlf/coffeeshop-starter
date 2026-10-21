package ek.osnb.demo.coffeeshop.order;

import ek.osnb.demo.coffeeshop.common.ConflictException;
import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit tests for the Order domain object: no Spring, no database, no Docker.
 */
class OrderTest {

    // ---------- Placing ----------

    @Test
    void newOrderIsPlaced() {
        Order order = newOrder();

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    void totalIsTheSumOfTheLines() {
        // 2 x 119.00 + 1 x 49.00
        Order order = newOrder();

        assertEquals(new BigDecimal("287.00"), order.getTotal());
    }

    @Test
    void itemCountAddsUpTheQuantities() {
        Order order = newOrder();

        assertEquals(3, order.itemCount());
    }

    @Test
    void placeRejectsAnOrderWithoutLines() {
        assertThrows(InvalidInputException.class,
                () -> Order.place(1L, address(), List.of(), Instant.now()));
    }

    @Test
    void lineTotalIsPriceTimesQuantity() {
        OrderLine line = new OrderLine(10L, "Ethiopia Yirgacheffe", new BigDecimal("119.00"), 3);

        assertEquals(new BigDecimal("357.00"), line.lineTotal());
    }

    @Test
    void lineRejectsZeroQuantity() {
        assertThrows(InvalidInputException.class,
                () -> new OrderLine(10L, "Ethiopia Yirgacheffe", new BigDecimal("119.00"), 0));
    }

    // ---------- Valid status changes ----------

    @Test
    void placedOrderCanBeCancelled() {
        Order order = newOrder();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void placedOrderCanBeShipped() {
        Order order = newOrder();

        order.ship();

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    // ---------- Invalid status changes ----------

    @Test
    void shippedOrderCannotBeCancelledAndStaysShipped() {
        Order order = newOrder();
        order.ship();

        assertThrows(ConflictException.class, () -> order.cancel());

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void cancelledOrderCannotBeCancelledAgain() {
        Order order = newOrder();
        order.cancel();

        assertThrows(ConflictException.class, () -> order.cancel());
    }

    @Test
    void cancelledOrderCannotBeShipped() {
        Order order = newOrder();
        order.cancel();

        assertThrows(ConflictException.class, () -> order.ship());
    }

    // ---------- Helpers ----------

    private static Order newOrder() {
        List<OrderLine> lines = List.of(
                new OrderLine(10L, "Ethiopia Yirgacheffe", new BigDecimal("119.00"), 2),
                new OrderLine(20L, "Paper Filters (100 pcs)", new BigDecimal("49.00"), 1)
        );

        return Order.place(1L, address(), lines, Instant.parse("2026-10-20T10:00:00Z"));
    }

    private static ShippingAddress address() {
        return new ShippingAddress("Anna Jensen", "Nørregade 1", "1165", "København K");
    }
}
