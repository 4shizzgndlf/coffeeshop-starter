package ek.osnb.demo.coffeeshop.cart;

/**
 * The cart's public contract: the only way other features use the cart.
 * Implemented by {@link CartService}.
 */
public interface CartApi {

    /**
     * @throws ek.osnb.demo.coffeeshop.common.NotFoundException if there is no such cart
     */
    CartInfo getCart(Long cartId);

    /** Removes every item, e.g. after the cart has become an order. */
    void clearCart(Long cartId);
}
