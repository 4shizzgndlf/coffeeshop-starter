package ek.osnb.demo.coffeeshop.order;

import ek.osnb.demo.coffeeshop.common.ConflictException;
import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The table is "orders": ORDER is a reserved word in SQL (ORDER BY)
@Entity
@Table(name = "orders")
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The customer belongs to another feature: store the id only, no @ManyToOne Customer
    @Column(nullable = false)
    private Long customerId;

    @Embedded
    private ShippingAddress shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant placedAt;

    // Stored, not calculated on every read: the lines never change after placing
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
        // Required by JPA
    }

    private Order(Long customerId, ShippingAddress shippingAddress, List<OrderLine> lines, Instant placedAt) {
        if (customerId == null) {
            throw new InvalidInputException("An order must have a customer");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidInputException("An order must have at least one line");
        }

        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.lines.addAll(lines);
        this.placedAt = placedAt;
        this.status = OrderStatus.PLACED;
        this.total = calculateTotal(lines);
    }

    static Order place(Long customerId, ShippingAddress shippingAddress, List<OrderLine> lines, Instant placedAt) {
        return new Order(customerId, shippingAddress, lines, placedAt);
    }

    void cancel() {
        if (status != OrderStatus.PLACED) {
            throw new ConflictException("Only placed orders can be cancelled");
        }

        status = OrderStatus.CANCELLED;
    }

    void ship() {
        if (status != OrderStatus.PLACED) {
            throw new ConflictException("Only placed orders can be shipped");
        }

        status = OrderStatus.SHIPPED;
    }

    int itemCount() {
        int count = 0;

        for (OrderLine line : lines) {
            count += line.getQuantity();
        }

        return count;
    }

    private static BigDecimal calculateTotal(List<OrderLine> lines) {
        BigDecimal sum = BigDecimal.ZERO;

        for (OrderLine line : lines) {
            sum = sum.add(line.lineTotal());
        }

        return sum;
    }

    Long getId() {
        return id;
    }

    Long getCustomerId() {
        return customerId;
    }

    ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    OrderStatus getStatus() {
        return status;
    }

    Instant getPlacedAt() {
        return placedAt;
    }

    BigDecimal getTotal() {
        return total;
    }

    List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
