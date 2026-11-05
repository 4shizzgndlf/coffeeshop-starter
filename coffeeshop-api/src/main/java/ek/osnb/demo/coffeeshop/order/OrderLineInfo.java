package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;

/** One line of an {@link OrderInfo}, with the name and price as they were when it was ordered. */
public record OrderLineInfo(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
