package ek.osnb.demo.coffeeshop.customer;

/**
 * What other features may know about a customer. Returned by {@link CustomerApi}.
 */
public record CustomerInfo(
        Long id,
        String name,
        String email,
        String street,
        String postalCode,
        String city
) {
}
