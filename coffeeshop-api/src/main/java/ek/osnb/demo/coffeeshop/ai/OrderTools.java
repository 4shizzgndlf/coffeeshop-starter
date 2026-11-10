package ek.osnb.demo.coffeeshop.ai;

import ek.osnb.demo.coffeeshop.order.OrderApi;
import ek.osnb.demo.coffeeshop.order.OrderInfo;
import ek.osnb.demo.coffeeshop.order.OrderSummaryInfo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/** Read-only order tools. The customer id always comes from the server's tool context. */
@Component
class OrderTools {

    private final OrderApi orders;

    OrderTools(OrderApi orders) {
        this.orders = orders;
    }

    @Tool(description = "List the logged-in customer's own orders, newest first.")
    List<OrderSummaryInfo> myOrders(ToolContext context) {
        return orders.findOrders(customerId(context));
    }

    @Tool(description = "Show one of the logged-in customer's own orders, including its items and delivery address.")
    OrderInfo myOrder(Long orderId, ToolContext context) {
        return orders.getOrder(customerId(context), orderId);
    }

    private Long customerId(ToolContext context) {
        Object value = context.getContext().get("customerId");
        if (value instanceof Long customerId) {
            return customerId;
        }
        throw new IllegalStateException("Customer id is missing from the assistant tool context");
    }
}
