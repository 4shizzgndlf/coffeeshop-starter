package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit tests for the Product domain object: no Spring, no database, no Docker.
 */
class ProductTest {

    // ---------- Creating ----------

    @Test
    void createTrimsTheName() {
        Product product = Product.create("  Kenya Nyeri AA  ", null, beans(), "Kenya",
                RoastLevel.LIGHT, new BigDecimal("135.00"), 5);

        assertEquals("Kenya Nyeri AA", product.getName());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(InvalidInputException.class,
                () -> Product.create(" ", null, beans(), null, null, new BigDecimal("10.00"), 1));
    }

    @Test
    void createRejectsNegativePrice() {
        assertThrows(InvalidInputException.class,
                () -> Product.create("Beans", null, beans(), null, null, new BigDecimal("-1.00"), 1));
    }

    @Test
    void createRejectsNegativeStock() {
        assertThrows(InvalidInputException.class,
                () -> Product.create("Beans", null, beans(), null, null, new BigDecimal("10.00"), -1));
    }

    // ---------- Reserving stock ----------

    @Test
    void reserveStockTakesItemsOutOfStock() {
        Product product = productWithStock(10);

        product.reserveStock(3);

        assertEquals(7, product.getStock());
    }

    @Test
    void reservingTheLastItemsEmptiesTheStock() {
        Product product = productWithStock(2);

        product.reserveStock(2);

        assertEquals(0, product.getStock());
    }

    @Test
    void reservingMoreThanInStockIsRejectedAndKeepsTheStock() {
        Product product = productWithStock(2);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> product.reserveStock(3));

        assertEquals("Only 2 left of Colombia Huila", ex.getMessage());
        assertEquals(2, product.getStock());
    }

    @Test
    void reservingFromAnEmptyStockSaysOutOfStock() {
        Product product = productWithStock(0);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> product.reserveStock(1));

        assertEquals("Colombia Huila is out of stock", ex.getMessage());
    }

    @Test
    void reserveStockRejectsZeroQuantity() {
        Product product = productWithStock(10);

        assertThrows(InvalidInputException.class, () -> product.reserveStock(0));
    }

    // ---------- Releasing stock ----------

    @Test
    void releaseStockPutsItemsBack() {
        Product product = productWithStock(10);
        product.reserveStock(4);

        product.releaseStock(4);

        assertEquals(10, product.getStock());
    }

    @Test
    void releaseStockRejectsNegativeQuantity() {
        Product product = productWithStock(10);

        assertThrows(InvalidInputException.class, () -> product.releaseStock(-1));
    }

    // ---------- Helpers ----------

    private static Category beans() {
        return Category.create("Coffee beans");
    }

    private static Product productWithStock(int stock) {
        return Product.create("Colombia Huila", "Caramel and red apple", beans(), "Colombia",
                RoastLevel.MEDIUM, new BigDecimal("99.00"), stock);
    }
}
