package ek.osnb.demo.coffeeshop.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The catalog's public contract: the only way other features use the catalog.
 *
 * Other features store a productId and ask through this interface. They never
 * see the Product entity or the ProductRepository (both are package-private).
 * Implemented by {@link ProductService}.
 */
public interface CatalogApi {

    /**
     * @throws ek.osnb.demo.coffeeshop.common.NotFoundException if there is no such product
     */
    ProductInfo getProduct(Long productId);

    /**
     * Looks up many products in one query, e.g. every product in a cart.
     * Unknown ids are left out of the result.
     */
    Map<Long, ProductInfo> findProducts(Collection<Long> productIds);

    /**
     * Takes {@code quantity} items out of stock.
     *
     * @throws InsufficientStockException if there are not enough in stock
     */
    void reserveStock(Long productId, int quantity);

    /** Puts {@code quantity} items back in stock. */
    void releaseStock(Long productId, int quantity);

    // ---------- Read-only, with everything a customer can see ----------

    /** Every category, sorted by name. */
    List<CategoryInfo> listCategories();

    /**
     * Products sorted by name. Both filters are optional: null (or a blank text) matches everything.
     *
     * @param categoryId only products in this category
     * @param text       part of the name or origin, ignoring case, e.g. "kenya"
     */
    List<ProductDetailsInfo> searchProducts(Long categoryId, String text);

    /**
     * @throws ek.osnb.demo.coffeeshop.common.NotFoundException if there is no such product
     */
    ProductDetailsInfo getProductDetails(Long productId);
}
