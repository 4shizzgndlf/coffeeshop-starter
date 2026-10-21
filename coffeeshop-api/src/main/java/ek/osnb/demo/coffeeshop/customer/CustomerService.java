package ek.osnb.demo.coffeeshop.customer;

import ek.osnb.demo.coffeeshop.common.ConflictException;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
class CustomerService implements CustomerApi {

    private final CustomerRepository repository;

    CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    // ---------- Used by CustomerController ----------

    CustomerResponse create(CustomerRequest request) {
        requireEmailIsFree(request.email());

        Customer customer = Customer.create(
                request.name(),
                request.email(),
                request.street(),
                request.postalCode(),
                request.city()
        );

        return CustomerMapper.toResponse(repository.save(customer));
    }

    @Transactional(readOnly = true)
    CustomerResponse findById(Long id) {
        return CustomerMapper.toResponse(findCustomer(id));
    }

    // A list with zero or one customer. A stand-in for logging in until the
    // app has Spring Security: anyone who knows an email can find the customer.
    @Transactional(readOnly = true)
    List<CustomerResponse> findByEmail(String email) {
        return repository.findByEmail(Customer.normalizeEmail(email))
                .map(CustomerMapper::toResponse)
                .stream()
                .toList();
    }

    CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);

        // Changing the email is fine, but not to one another customer already uses
        if (!customer.getEmail().equals(Customer.normalizeEmail(request.email()))) {
            requireEmailIsFree(request.email());
        }

        customer.updateDetails(
                request.name(),
                request.email(),
                request.street(),
                request.postalCode(),
                request.city()
        );

        return CustomerMapper.toResponse(customer);
    }

    // ---------- CustomerApi: used by other features ----------

    @Override
    @Transactional(readOnly = true)
    public CustomerInfo getCustomer(Long customerId) {
        return CustomerMapper.toInfo(findCustomer(customerId));
    }

    private void requireEmailIsFree(String email) {
        if (repository.existsByEmail(Customer.normalizeEmail(email))) {
            throw new ConflictException("A customer with this email already exists");
        }
    }

    private Customer findCustomer(Long id) {
        Optional<Customer> result = repository.findById(id);

        if (result.isEmpty()) {
            throw new NotFoundException("Customer not found: " + id);
        }

        return result.get();
    }
}
