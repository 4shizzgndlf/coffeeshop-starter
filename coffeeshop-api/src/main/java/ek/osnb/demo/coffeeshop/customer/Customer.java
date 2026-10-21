package ek.osnb.demo.coffeeshop.customer;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import jakarta.persistence.*;

import java.util.Locale;

@Entity
@Table(name = "customers")
class Customer {

    static final int NAME_MAX_LENGTH = 100;
    static final int EMAIL_MAX_LENGTH = 254;
    static final int STREET_MAX_LENGTH = 120;
    static final int POSTAL_CODE_MAX_LENGTH = 10;
    static final int CITY_MAX_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    // Unique: one customer per email address
    @Column(nullable = false, unique = true, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false, length = STREET_MAX_LENGTH)
    private String street;

    @Column(nullable = false, length = POSTAL_CODE_MAX_LENGTH)
    private String postalCode;

    @Column(nullable = false, length = CITY_MAX_LENGTH)
    private String city;

    protected Customer() {
        // Required by JPA
    }

    private Customer(String name, String email, String street, String postalCode, String city) {
        updateDetails(name, email, street, postalCode, city);
    }

    static Customer create(String name, String email, String street, String postalCode, String city) {
        return new Customer(name, email, street, postalCode, city);
    }

    void updateDetails(String name, String email, String street, String postalCode, String city) {
        // Validate every value before changing any field,
        // so an invalid value never leaves a half-updated customer.
        String validName = required(name, "name");
        String validEmail = normalizeEmail(required(email, "email"));
        String validStreet = required(street, "street");
        String validPostalCode = required(postalCode, "postal code");
        String validCity = required(city, "city");

        this.name = validName;
        this.email = validEmail;
        this.street = validStreet;
        this.postalCode = validPostalCode;
        this.city = validCity;
    }

    // "  Anna@Example.COM " and "anna@example.com" are the same address
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("A customer must have a " + field);
        }

        return value.trim();
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
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
