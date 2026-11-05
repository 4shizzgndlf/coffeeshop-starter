package ek.osnb.demo.coffeeshop.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CustomerApi.findCustomerByEmail with the real application and H2.
 * Each test is rolled back afterwards ({@code @Transactional}).
 */
@SpringBootTest
@Transactional
class CustomerApiTest {

    @Autowired
    private CustomerApi customers;

    @Autowired
    private CustomerRepository repository;

    @Test
    void findsTheCustomerByEmailIgnoringCaseAndSpaces() {
        Customer anna = repository.save(Customer.create(
                "Anna Jensen", "anna.api-test@example.com", "Nørregade 1", "1165", "København K"));

        assertThat(customers.findCustomerByEmail("  Anna.API-Test@Example.com "))
                .hasValueSatisfying(info -> {
                    assertThat(info.id()).isEqualTo(anna.getId());
                    assertThat(info.city()).isEqualTo("København K");
                });
    }

    @Test
    void unknownEmailIsEmpty() {
        assertThat(customers.findCustomerByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void nullEmailIsEmpty() {
        assertThat(customers.findCustomerByEmail(null)).isEmpty();
    }
}
