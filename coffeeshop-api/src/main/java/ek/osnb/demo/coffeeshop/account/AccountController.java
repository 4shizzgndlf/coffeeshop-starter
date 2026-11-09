package ek.osnb.demo.coffeeshop.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountController {

    private final AccountService service;

    AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterRequest request) {
        service.register(request);
    }
}
