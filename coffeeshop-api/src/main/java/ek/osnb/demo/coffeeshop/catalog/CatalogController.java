package ek.osnb.demo.coffeeshop.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only: products are added by CatalogInitData. Managing products
// (POST/PUT/DELETE) belongs behind a login - a good feature to add with Spring Security.
@RestController
@RequestMapping("/api")
class CatalogController {

    private final ProductService service;

    CatalogController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    List<CategoryResponse> findCategories() {
        return service.findCategories();
    }

    // GET /api/products?categoryId=1&q=ethiopia - both parameters are optional
    @GetMapping("/products")
    List<ProductResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(name = "q", required = false) String search) {

        return service.search(categoryId, search);
    }

    @GetMapping("/products/{id}")
    ProductResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
