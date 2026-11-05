package ek.osnb.demo.coffeeshop.catalog;

import java.math.BigDecimal;

/**
 * Everything a customer can read about a product, e.g. for an AI assistant
 * that recommends coffee. {@link ProductInfo} is the short version that the
 * cart and the order use.
 *
 * @param category   the category's name, e.g. "Coffee beans"
 * @param origin     null for equipment
 * @param roastLevel "LIGHT", "MEDIUM" or "DARK"; null for equipment
 */
public record ProductDetailsInfo(
        Long id,
        String name,
        String description,
        String category,
        String origin,
        String roastLevel,
        BigDecimal price,
        int stock
) {
}
