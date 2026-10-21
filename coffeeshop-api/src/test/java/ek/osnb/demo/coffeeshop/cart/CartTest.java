package ek.osnb.demo.coffeeshop.cart;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for the Cart domain object: no Spring, no database, no Docker.
 */
class CartTest {

    private static final Long BEANS = 10L;
    private static final Long FILTERS = 20L;

    // ---------- Adding ----------

    @Test
    void newCartIsEmpty() {
        Cart cart = newCart();

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void addItemAddsALine() {
        Cart cart = newCart();

        cart.addItem(BEANS, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.quantityOf(BEANS));
    }

    @Test
    void addingTheSameProductAgainAddsToTheQuantity() {
        Cart cart = newCart();

        cart.addItem(BEANS, 2);
        cart.addItem(BEANS, 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.quantityOf(BEANS));
    }

    @Test
    void addItemRejectsZeroQuantity() {
        Cart cart = newCart();

        assertThrows(InvalidInputException.class, () -> cart.addItem(BEANS, 0));
    }

    @Test
    void addItemRejectsMoreThanTheMaximumInTotalAndKeepsTheQuantity() {
        Cart cart = newCart();
        cart.addItem(BEANS, 60);

        assertThrows(InvalidInputException.class, () -> cart.addItem(BEANS, 40));

        assertEquals(60, cart.quantityOf(BEANS));
    }

    // ---------- Changing and removing ----------

    @Test
    void changeQuantitySetsTheQuantity() {
        Cart cart = newCart();
        cart.addItem(BEANS, 2);

        cart.changeQuantity(BEANS, 7);

        assertEquals(7, cart.quantityOf(BEANS));
    }

    @Test
    void changeQuantityOfAProductNotInTheCartIsRejected() {
        Cart cart = newCart();

        assertThrows(NotFoundException.class, () -> cart.changeQuantity(BEANS, 1));
    }

    @Test
    void removeItemRemovesOnlyThatProduct() {
        Cart cart = newCart();
        cart.addItem(BEANS, 2);
        cart.addItem(FILTERS, 1);

        cart.removeItem(BEANS);

        assertEquals(0, cart.quantityOf(BEANS));
        assertEquals(1, cart.quantityOf(FILTERS));
    }

    @Test
    void removingAProductNotInTheCartDoesNothing() {
        Cart cart = newCart();
        cart.addItem(BEANS, 2);

        cart.removeItem(FILTERS);

        assertEquals(1, cart.getItems().size());
    }

    @Test
    void clearRemovesEverything() {
        Cart cart = newCart();
        cart.addItem(BEANS, 2);
        cart.addItem(FILTERS, 1);

        cart.clear();

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void itemsCannotBeChangedFromOutsideTheCart() {
        Cart cart = newCart();

        assertThrows(UnsupportedOperationException.class,
                () -> cart.getItems().add(new CartItem(BEANS, 1)));
    }

    // ---------- Helpers ----------

    private static Cart newCart() {
        return Cart.create(Instant.parse("2026-10-20T10:00:00Z"));
    }
}
