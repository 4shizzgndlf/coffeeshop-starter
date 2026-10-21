package ek.osnb.demo.coffeeshop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        Instant placedAt,
        ShippingAddressResponse shippingAddress,
        List<OrderLineResponse> lines,
        BigDecimal total
) {
}
