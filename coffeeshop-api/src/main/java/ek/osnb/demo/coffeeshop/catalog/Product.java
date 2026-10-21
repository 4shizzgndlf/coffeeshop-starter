package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
class Product {

    static final int NAME_MAX_LENGTH = 100;
    static final int DESCRIPTION_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    // Category is part of the catalog feature, so a JPA association is fine here.
    // Other features never do this: they store a productId (see CartItem, OrderLine).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    // Where the beans come from, e.g. "Ethiopia". Empty for equipment.
    @Column(length = 60)
    private String origin;

    // Empty for equipment
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoastLevel roastLevel;

    // Danish kroner. BigDecimal, never double, for money: 0.1 + 0.2 != 0.3 with doubles.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    // Optimistic locking: if two orders change the stock of the same product at the
    // same time, the second one fails instead of silently overwriting the first.
    @Version
    private Long version;

    protected Product() {
        // Required by JPA
    }

    private Product(
            String name,
            String description,
            Category category,
            String origin,
            RoastLevel roastLevel,
            BigDecimal price,
            int stock) {

        if (name == null || name.isBlank()) {
            throw new InvalidInputException("A product must have a name");
        }
        if (category == null) {
            throw new InvalidInputException("A product must have a category");
        }
        if (price == null || price.signum() < 0) {
            throw new InvalidInputException("A product must have a price of 0 or more");
        }
        if (stock < 0) {
            throw new InvalidInputException("Stock cannot be negative");
        }

        this.name = name.trim();
        this.description = description;
        this.category = category;
        this.origin = origin;
        this.roastLevel = roastLevel;
        this.price = price;
        this.stock = stock;
    }

    static Product create(
            String name,
            String description,
            Category category,
            String origin,
            RoastLevel roastLevel,
            BigDecimal price,
            int stock) {

        return new Product(name, description, category, origin, roastLevel, price, stock);
    }

    // Takes items out of stock when an order is placed
    void reserveStock(int quantity) {
        requirePositive(quantity);

        if (quantity > stock) {
            throw new InsufficientStockException(name, stock);
        }

        stock -= quantity;
    }

    // Puts items back in stock, e.g. when an order is cancelled
    void releaseStock(int quantity) {
        requirePositive(quantity);
        stock += quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be at least 1");
        }
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    Category getCategory() {
        return category;
    }

    String getOrigin() {
        return origin;
    }

    RoastLevel getRoastLevel() {
        return roastLevel;
    }

    BigDecimal getPrice() {
        return price;
    }

    int getStock() {
        return stock;
    }
}
