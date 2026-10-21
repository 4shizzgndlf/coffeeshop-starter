package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by OrderService when an order has been placed.
 *
 * Nothing in the base app listens to it. It is the way for new features to react
 * to orders without changing the order feature, e.g. loyalty points:
 *
 * <pre>
 * &#64;Component
 * class LoyaltyListener {
 *     &#64;TransactionalEventListener   // runs after the order is committed
 *     void on(OrderPlacedEvent event) { ... }
 * }
 * </pre>
 *
 * The new feature depends on order, and order knows nothing about it.
 */
public record OrderPlacedEvent(
        Long orderId,
        Long customerId,
        BigDecimal total,
        Instant placedAt
) {
}
