package ek.osnb.demo.coffeeshop.cart;

import java.math.BigDecimal;
import java.util.List;

record CartResponse(
        Long id,
        List<CartLineResponse> lines,
        int totalQuantity,
        BigDecimal total
) {
}
