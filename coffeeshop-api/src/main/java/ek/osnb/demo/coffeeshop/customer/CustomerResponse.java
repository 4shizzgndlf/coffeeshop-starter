package ek.osnb.demo.coffeeshop.customer;

record CustomerResponse(
        Long id,
        String name,
        String email,
        String street,
        String postalCode,
        String city
) {
}
