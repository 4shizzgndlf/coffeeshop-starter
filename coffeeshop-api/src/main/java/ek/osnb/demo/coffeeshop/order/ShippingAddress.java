package ek.osnb.demo.coffeeshop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Where the order is sent. A copy of the customer's address when the order was placed:
 * if the customer moves later, old orders still show where they were actually sent.
 *
 * {@code @Embeddable}: no table of its own - the fields become columns in "orders".
 */
@Embeddable
class ShippingAddress {

    @Column(name = "ship_to_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "ship_to_street", nullable = false, length = 120)
    private String street;

    @Column(name = "ship_to_postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "ship_to_city", nullable = false, length = 60)
    private String city;

    protected ShippingAddress() {
        // Required by JPA
    }

    ShippingAddress(String recipientName, String street, String postalCode, String city) {
        this.recipientName = recipientName;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
    }

    String getRecipientName() {
        return recipientName;
    }

    String getStreet() {
        return street;
    }

    String getPostalCode() {
        return postalCode;
    }

    String getCity() {
        return city;
    }
}
