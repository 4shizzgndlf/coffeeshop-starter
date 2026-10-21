package ek.osnb.demo.coffeeshop.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Used both to create (POST) and to update (PUT) a customer
record CustomerRequest(

        @NotBlank
        @Size(max = Customer.NAME_MAX_LENGTH)
        String name,

        @NotBlank
        @Email
        @Size(max = Customer.EMAIL_MAX_LENGTH)
        String email,

        @NotBlank
        @Size(max = Customer.STREET_MAX_LENGTH)
        String street,

        @NotBlank
        @Size(max = Customer.POSTAL_CODE_MAX_LENGTH)
        String postalCode,

        @NotBlank
        @Size(max = Customer.CITY_MAX_LENGTH)
        String city
) {
}
