package ek.osnb.demo.coffeeshop.customer;

final class CustomerMapper {

    private CustomerMapper() {
    }

    static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getStreet(),
                customer.getPostalCode(),
                customer.getCity()
        );
    }

    static CustomerInfo toInfo(Customer customer) {
        return new CustomerInfo(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getStreet(),
                customer.getPostalCode(),
                customer.getCity()
        );
    }
}
