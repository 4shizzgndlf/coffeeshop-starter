package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;

record OrderLineResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
