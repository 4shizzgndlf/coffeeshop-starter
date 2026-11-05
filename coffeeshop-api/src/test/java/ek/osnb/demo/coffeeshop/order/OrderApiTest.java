package ek.osnb.demo.coffeeshop.order;

import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests OrderApi with the real application and H2. The orders are saved straight
 * through the repository, so these tests don't depend on the REST API.
 *
 * {@code @Transactional}: each test is rolled back afterwards, so its orders
 * don't show up in other tests. The customer ids are only numbers here: an order
 * stores the id, and nothing checks that the customer exists.
 */
@SpringBootTest
@Transactional
class OrderApiTest {

    private static final long ANNA = 900_001L;
    private static final long BO = 900_002L;

    @Autowired
    private OrderApi orders;

    @Autowired
    private OrderRepository repository;

    @Test
    void findsOnlyTheCustomersOwnOrdersNewestFirst() {
        Order older = saveOrder(ANNA, Instant.parse("2026-11-01T10:00:00Z"));
        Order newer = saveOrder(ANNA, Instant.parse("2026-11-02T10:00:00Z"));
        saveOrder(BO, Instant.parse("2026-11-03T10:00:00Z"));

        List<OrderSummaryInfo> result = orders.findOrders(ANNA);

        assertThat(result).extracting(OrderSummaryInfo::id).containsExactly(newer.getId(), older.getId());
        assertThat(result.getFirst().status()).isEqualTo("PLACED");
        assertThat(result.getFirst().itemCount()).isEqualTo(3);
        assertThat(result.getFirst().total()).isEqualByComparingTo("287.00");
    }

    @Test
    void customerWithoutOrdersGetsAnEmptyList() {
        assertThat(orders.findOrders(BO)).isEmpty();
    }

    @Test
    void getsOneOrderWithLinesAndAddress() {
        Order order = saveOrder(ANNA, Instant.now());

        OrderInfo info = orders.getOrder(ANNA, order.getId());

        assertThat(info.shipTo()).isEqualTo("Anna Jensen, Nørregade 1, 1165 København K");
        assertThat(info.lines()).extracting(OrderLineInfo::productName)
                .containsExactly("Kenya Nyeri AA", "Paper Filters (100 pcs)");
        assertThat(info.lines().getFirst().lineTotal()).isEqualByComparingTo("238.00");
        assertThat(info.total()).isEqualByComparingTo("287.00");
    }

    @Test
    void anotherCustomersOrderIsNotFound() {
        Order annasOrder = saveOrder(ANNA, Instant.now());

        // The same answer as for an order that doesn't exist
        assertThatThrownBy(() -> orders.getOrder(BO, annasOrder.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found: " + annasOrder.getId());
    }

    @Test
    void cancelledOrderShowsItsStatus() {
        Order order = saveOrder(ANNA, Instant.now());
        order.cancel();

        assertThat(orders.getOrder(ANNA, order.getId()).status()).isEqualTo("CANCELLED");
    }

    // 2 x Kenya Nyeri AA (119.00) + 1 x Paper Filters (49.00) = 287.00
    private Order saveOrder(long customerId, Instant placedAt) {
        ShippingAddress address = new ShippingAddress("Anna Jensen", "Nørregade 1", "1165", "København K");
        List<OrderLine> lines = List.of(
                new OrderLine(2L, "Kenya Nyeri AA", new BigDecimal("119.00"), 2),
                new OrderLine(14L, "Paper Filters (100 pcs)", new BigDecimal("49.00"), 1)
        );

        return repository.save(Order.place(customerId, address, lines, placedAt));
    }
}
