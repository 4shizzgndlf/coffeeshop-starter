package ek.osnb.demo.coffeeshop.order;

import jakarta.validation.constraints.NotNull;

// The controller ignores customerId and gets the customer from the authenticated session.
// It remains here only for package-local legacy service tests while the old tests are migrated.
record PlaceOrderRequest(

        Long customerId,

        @NotNull
        Long cartId
) {
    PlaceOrderRequest(Long cartId) {
        this(null, cartId);
    }
}
