package ek.osnb.demo.coffeeshop.cart;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A shopping cart. Not tied to a customer: the browser keeps the cart id
 * (in localStorage) until the customer checks out.
 *
 * Cart and CartItem belong to the same feature, so a JPA association is fine.
 * Items are only changed through the cart, which keeps the rules in one place.
 */
@Entity
@Table(name = "carts")
class Cart {

    static final int MAX_QUANTITY = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    // cascade: saving the cart saves its items.
    // orphanRemoval: an item removed from the list is deleted from the database.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cart_id", nullable = false)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
        // Required by JPA
    }

    private Cart(Instant createdAt) {
        this.createdAt = createdAt;
    }

    static Cart create(Instant createdAt) {
        return new Cart(createdAt);
    }

    // Adding a product that is already in the cart adds to its quantity
    void addItem(Long productId, int quantity) {
        requireValidQuantity(quantity);

        CartItem existing = findItem(productId);

        if (existing == null) {
            items.add(new CartItem(productId, quantity));
        } else {
            int newQuantity = existing.getQuantity() + quantity;
            requireValidQuantity(newQuantity);
            existing.changeQuantity(newQuantity);
        }
    }

    void changeQuantity(Long productId, int quantity) {
        requireValidQuantity(quantity);

        CartItem item = findItem(productId);

        if (item == null) {
            throw new NotFoundException("Product " + productId + " is not in the cart");
        }

        item.changeQuantity(quantity);
    }

    // Removing a product that is not in the cart does nothing
    void removeItem(Long productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    void clear() {
        items.clear();
    }

    int quantityOf(Long productId) {
        CartItem item = findItem(productId);
        return item == null ? 0 : item.getQuantity();
    }

    private CartItem findItem(Long productId) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                return item;
            }
        }

        return null;
    }

    private static void requireValidQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidInputException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
    }

    Long getId() {
        return id;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    // Read-only: use addItem, changeQuantity and removeItem to change the items
    List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
