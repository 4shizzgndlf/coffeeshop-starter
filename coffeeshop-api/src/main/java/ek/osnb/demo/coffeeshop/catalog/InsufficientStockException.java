package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.ConflictException;

/**
 * Thrown when more items are wanted than are in stock.
 * Public, because the cart and order features can run into it too.
 * Mapped to 409 Conflict through {@link ConflictException}.
 */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String productName, int available) {
        super(available == 0
                ? productName + " is out of stock"
                : "Only " + available + " left of " + productName);
    }
}
