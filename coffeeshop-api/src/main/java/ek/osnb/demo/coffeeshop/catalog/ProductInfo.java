package ek.osnb.demo.coffeeshop.catalog;

import java.math.BigDecimal;

/**
 * What other features may know about a product. Returned by {@link CatalogApi}.
 *
 * A read-only copy, not the Product entity: other features cannot change a
 * product by accident, and the catalog can change its entity without breaking them.
 */
public record ProductInfo(
        Long id,
        String name,
        BigDecimal price,
        int stock
) {
}
