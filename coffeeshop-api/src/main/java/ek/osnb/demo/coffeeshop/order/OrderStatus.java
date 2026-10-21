package ek.osnb.demo.coffeeshop.order;

// PLACED -> SHIPPED, or PLACED -> CANCELLED. Nothing happens after SHIPPED or CANCELLED.
enum OrderStatus {
    PLACED,
    SHIPPED,
    CANCELLED
}
