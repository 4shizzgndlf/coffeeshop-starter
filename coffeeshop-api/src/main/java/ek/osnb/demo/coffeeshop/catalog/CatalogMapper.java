package ek.osnb.demo.coffeeshop.catalog;

final class CatalogMapper {

    private CatalogMapper() {
    }

    static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getOrigin(),
                product.getRoastLevel(),
                product.getPrice(),
                product.getStock()
        );
    }

    // For other features (through CatalogApi): only what they need
    static ProductInfo toInfo(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
