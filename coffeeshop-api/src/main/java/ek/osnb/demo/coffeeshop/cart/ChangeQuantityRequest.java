package ek.osnb.demo.coffeeshop.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// To remove a product, use DELETE /api/carts/{id}/items/{productId} instead of quantity 0
record ChangeQuantityRequest(

        @Min(1)
        @Max(Cart.MAX_QUANTITY)
        int quantity
) {
}
