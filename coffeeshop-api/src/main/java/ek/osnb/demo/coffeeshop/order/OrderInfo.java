package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One order with its lines. Returned by {@link OrderApi#getOrder(Long, Long)}.
 *
 * @param status "PLACED", "SHIPPED" or "CANCELLED"
 * @param shipTo the delivery address in one line, e.g. "Anna Jensen, Nørregade 1, 1165 København K"
 */
public record OrderInfo(
        Long id,
        String status,
        Instant placedAt,
        String shipTo,
        List<OrderLineInfo> lines,
        BigDecimal total
) {
}
