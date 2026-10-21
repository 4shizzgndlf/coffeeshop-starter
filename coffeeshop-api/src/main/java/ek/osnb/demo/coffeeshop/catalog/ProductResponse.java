package ek.osnb.demo.coffeeshop.catalog;

import java.math.BigDecimal;

record ProductResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String origin,
        RoastLevel roastLevel,
        BigDecimal price,
        int stock
) {
}
