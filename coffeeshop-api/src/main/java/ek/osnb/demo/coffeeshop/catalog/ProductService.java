package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serves two kinds of callers:
 * - CatalogController (the REST API), with *Response records
 * - other features, through the public CatalogApi interface, with ProductInfo
 */
@Service
@Transactional
class ProductService implements CatalogApi {

    private final ProductRepository products;
    private final CategoryRepository categories;

    ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    // ---------- Used by CatalogController ----------

    @Transactional(readOnly = true)
    List<CategoryResponse> findCategories() {
        List<CategoryResponse> responses = new ArrayList<>();

        for (Category category : categories.findAll(Sort.by("name"))) {
            responses.add(CatalogMapper.toResponse(category));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    List<ProductResponse> search(Long categoryId, String search) {
        // Treat "?q=" (empty) the same as no search at all
        String text = (search == null || search.isBlank()) ? null : search.trim();

        List<ProductResponse> responses = new ArrayList<>();

        for (Product product : products.search(categoryId, text)) {
            responses.add(CatalogMapper.toResponse(product));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    ProductResponse findById(Long id) {
        return CatalogMapper.toResponse(findProduct(id));
    }

    // ---------- CatalogApi: used by other features ----------

    @Override
    @Transactional(readOnly = true)
    public ProductInfo getProduct(Long productId) {
        return CatalogMapper.toInfo(findProduct(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProductInfo> findProducts(Collection<Long> productIds) {
        Map<Long, ProductInfo> result = new HashMap<>();

        for (Product product : products.findAllById(productIds)) {
            result.put(product.getId(), CatalogMapper.toInfo(product));
        }

        return result;
    }

    // No save() needed: the product is managed, so JPA writes the new stock
    // when the transaction commits - or not at all if it is rolled back.
    @Override
    public void reserveStock(Long productId, int quantity) {
        findProduct(productId).reserveStock(quantity);
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        findProduct(productId).releaseStock(quantity);
    }

    private Product findProduct(Long id) {
        Optional<Product> result = products.findById(id);

        if (result.isEmpty()) {
            throw new NotFoundException("Product not found: " + id);
        }

        return result.get();
    }
}
