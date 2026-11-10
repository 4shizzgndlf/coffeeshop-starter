package ek.osnb.demo.coffeeshop.ai;

import ek.osnb.demo.coffeeshop.catalog.CatalogApi;
import ek.osnb.demo.coffeeshop.catalog.ProductDetailsInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/** Read-only inventory data available only in a staff assistant prompt. */
@Component
class StaffCatalogTools {

    private static final int LOW_STOCK_LIMIT = 5;

    private final CatalogApi catalog;

    StaffCatalogTools(CatalogApi catalog) {
        this.catalog = catalog;
    }

    @Tool(description = "List products running low: five or fewer items remain, including out-of-stock products.")
    List<ProductDetailsInfo> lowStockProducts() {
        return catalog.searchProducts(null, null).stream()
                .filter(product -> product.stock() <= LOW_STOCK_LIMIT)
                .toList();
    }
}
