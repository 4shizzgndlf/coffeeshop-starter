package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;
import java.time.Instant;

// One row in "My orders": no lines or address, just enough for the list
record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        Instant placedAt,
        int itemCount,
        BigDecimal total
) {
}
