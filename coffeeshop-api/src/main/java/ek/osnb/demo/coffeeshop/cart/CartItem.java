package ek.osnb.demo.coffeeshop.cart;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The product belongs to another feature (catalog), so we only keep its id:
    // no @ManyToOne Product. The name and price are looked up through CatalogApi.
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
        // Required by JPA
    }

    // Only Cart creates and changes items - it checks the quantity first
    CartItem(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    Long getId() {
        return id;
    }

    Long getProductId() {
        return productId;
    }

    int getQuantity() {
        return quantity;
    }
}
