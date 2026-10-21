package ek.osnb.demo.coffeeshop.cart;

/**
 * One line of a cart, as other features see it: which product and how many.
 * Part of {@link CartInfo}.
 */
public record CartLineInfo(
        Long productId,
        int quantity
) {
}
