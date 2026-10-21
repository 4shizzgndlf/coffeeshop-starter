package ek.osnb.demo.coffeeshop.cart;

import java.util.List;

/**
 * What other features may know about a cart. Returned by {@link CartApi}.
 * Only product ids and quantities: names and prices belong to the catalog.
 */
public record CartInfo(
        Long id,
        List<CartLineInfo> lines
) {

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
