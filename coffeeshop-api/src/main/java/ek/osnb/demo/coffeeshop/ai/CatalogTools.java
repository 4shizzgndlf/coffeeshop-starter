package ek.osnb.demo.coffeeshop.ai;

import ek.osnb.demo.coffeeshop.catalog.CatalogApi;
import ek.osnb.demo.coffeeshop.catalog.ProductDetailsInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/** Read-only product data that the model may use when answering customers. */
@Component
class CatalogTools {

    private final CatalogApi catalog;

    CatalogTools(CatalogApi catalog) {
        this.catalog = catalog;
    }

    @Tool(description = "Search the current catalog by product name or origin. "
            + "Returns names, descriptions, roast levels, prices, and stock. Use this before stating product facts.")
    List<ProductDetailsInfo> searchProducts(String searchText) {
        return catalog.searchProducts(null, searchText);
    }

    @Tool(description = "List every product with a specified roast level: LIGHT, MEDIUM, or DARK. "
            + "Use this for questions about light, medium, or dark roasts.")
    List<ProductDetailsInfo> findByRoastLevel(String roastLevel) {
        return catalog.searchProducts(null, null).stream()
                .filter(product -> roastLevel.equalsIgnoreCase(product.roastLevel()))
                .toList();
    }

    @Tool(description = "Get the current full details, price, and stock for a product by its id.")
    ProductDetailsInfo getProduct(Long productId) {
        return catalog.getProductDetails(productId);
    }
}
