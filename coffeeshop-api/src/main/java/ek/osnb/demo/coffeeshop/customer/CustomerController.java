package ek.osnb.demo.coffeeshop.customer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/customers")
class CustomerController {

    private final CustomerService service;

    CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping("/me")
    MyCustomerResponse getMyProfile(Authentication authentication) {
        return service.getMyProfile(authentication.getName());
    }

    @PutMapping("/me")
    MyCustomerResponse saveMyProfile(
            Authentication authentication,
            @Valid @RequestBody MyCustomerRequest request) {
        return service.saveMyProfile(authentication.getName(), request);
    }
}
