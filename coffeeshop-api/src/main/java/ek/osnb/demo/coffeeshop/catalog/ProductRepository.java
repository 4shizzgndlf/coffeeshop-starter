package ek.osnb.demo.coffeeshop.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ProductRepository extends JpaRepository<Product, Long> {

    // Both filters are optional: a null categoryId or search matches every product.
    // @EntityGraph loads each product's category in the same query (no N+1 queries).
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and (:search is null
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.origin) like lower(concat('%', :search, '%')))
            order by p.name
            """)
    List<Product> search(Long categoryId, String search);
}
