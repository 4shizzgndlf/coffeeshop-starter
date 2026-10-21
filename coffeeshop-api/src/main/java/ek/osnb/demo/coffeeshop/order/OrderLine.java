package ek.osnb.demo.coffeeshop.order;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * One product in an order.
 *
 * productId points to the catalog (by id only - no @ManyToOne Product).
 * productName and unitPrice are copies, taken when the order was placed:
 * when the catalog changes a price later, this order keeps the price that was paid.
 */
@Entity
@Table(name = "order_lines")
class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {
        // Required by JPA
    }

    OrderLine(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        if (quantity < 1) {
            throw new InvalidInputException("Quantity must be at least 1");
        }

        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    Long getId() {
        return id;
    }

    Long getProductId() {
        return productId;
    }

    String getProductName() {
        return productName;
    }

    BigDecimal getUnitPrice() {
        return unitPrice;
    }

    int getQuantity() {
        return quantity;
    }
}
