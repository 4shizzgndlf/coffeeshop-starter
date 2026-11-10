package ek.osnb.demo.coffeeshop.order;

import jakarta.validation.Valid;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import ek.osnb.demo.coffeeshop.customer.CustomerApi;
import ek.osnb.demo.coffeeshop.customer.CustomerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService service;
    private final CustomerApi customers;

    OrderController(OrderService service, CustomerApi customers) {
        this.service = service;
        this.customers = customers;
    }

    // Checkout: turns a cart into an order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse placeOrder(Authentication authentication, @Valid @RequestBody PlaceOrderRequest request) {
        return service.placeOrder(customer(authentication).id(), request);
    }

    @GetMapping
    List<OrderSummaryResponse> findByCustomer(Authentication authentication) {
        return customers.findCustomerByEmail(authentication.getName())
                .map(customer -> service.findByCustomer(customer.id()))
                .orElseGet(List::of);
    }

    @GetMapping("/{id}")
    OrderResponse findById(Authentication authentication, @PathVariable Long id) {
        if (isStaff(authentication)) {
            return service.findById(id);
        }
        return service.findByIdForCustomer(customer(authentication).id(), id);
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(Authentication authentication, @PathVariable Long id) {
        return service.cancel(customer(authentication).id(), id);
    }

    // Spring Security limits this endpoint to STAFF.
    @PostMapping("/{id}/ship")
    OrderResponse ship(@PathVariable Long id) {
        return service.ship(id);
    }

    private CustomerInfo customer(Authentication authentication) {
        return customers.findCustomerByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Customer profile not found"));
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF"));
    }
}
