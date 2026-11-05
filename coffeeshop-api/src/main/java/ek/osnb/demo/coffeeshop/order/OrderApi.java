package ek.osnb.demo.coffeeshop.order;

import java.util.List;

/**
 * The order feature's public contract: read-only access to one customer's orders,
 * e.g. for an AI assistant that answers "where is my order?".
 *
 * Every method takes the customer explicitly, so a caller can only see the orders
 * of the customer it asks for. Implemented by {@link OrderService}.
 */
public interface OrderApi {

    /** The customer's orders, newest first. Empty for a customer with no orders. */
    List<OrderSummaryInfo> findOrders(Long customerId);

    /**
     * One of the customer's orders, with its lines.
     *
     * @throws ek.osnb.demo.coffeeshop.common.NotFoundException if there is no such order,
     *         or it belongs to another customer (the caller is not told which)
     */
    OrderInfo getOrder(Long customerId, Long orderId);
}
