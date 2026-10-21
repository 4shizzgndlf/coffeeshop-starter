package ek.osnb.demo.coffeeshop.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record AddCartItemRequest(

        @NotNull
        Long productId,

        // A missing quantity becomes 0, which @Min rejects
        @Min(1)
        @Max(Cart.MAX_QUANTITY)
        int quantity
) {
}
