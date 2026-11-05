package ek.osnb.demo.coffeeshop.customer;

import java.util.Optional;

/**
 * The customer feature's public contract: the only way other features use it.
 * Other features store a customerId and look the customer up here.
 * Implemented by {@link CustomerService}.
 */
public interface CustomerApi {

    /**
     * @throws ek.osnb.demo.coffeeshop.common.NotFoundException if there is no such customer
     */
    CustomerInfo getCustomer(Long customerId);

    /**
     * The customer with this email, ignoring case, or empty if there is none.
     * Links a login (where the username is the email) to the customer's profile.
     */
    Optional<CustomerInfo> findCustomerByEmail(String email);
}
