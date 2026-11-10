package ek.osnb.demo.coffeeshop.customer;

record MyCustomerResponse(String name, String street, String postalCode, String city) {
    static MyCustomerResponse from(Customer customer) {
        return new MyCustomerResponse(
                customer.getName(), customer.getStreet(), customer.getPostalCode(), customer.getCity());
    }
}
