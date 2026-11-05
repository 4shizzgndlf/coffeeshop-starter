package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One order in a list, without its lines. Returned by {@link OrderApi#findOrders(Long)}.
 *
 * @param status "PLACED", "SHIPPED" or "CANCELLED"
 */
public record OrderSummaryInfo(
        Long id,
        String status,
        Instant placedAt,
        int itemCount,
        BigDecimal total
) {
}
