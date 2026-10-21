package ek.osnb.demo.coffeeshop.cart;

import java.math.BigDecimal;

// productName, unitPrice and available come from the catalog (live values, not copies)
record CartLineResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int available
) {
}
