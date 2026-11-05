package ek.osnb.demo.coffeeshop.catalog;

/** A category, as other features see it. Returned by {@link CatalogApi#listCategories()}. */
public record CategoryInfo(
        Long id,
        String name
) {
}
