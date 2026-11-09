package ek.osnb.demo.coffeeshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class MeController {

    @GetMapping("/api/me")
    MeResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();
        return new MeResponse(authentication.getName(), roles);
    }
}

record MeResponse(String username, List<String> roles) {
}
