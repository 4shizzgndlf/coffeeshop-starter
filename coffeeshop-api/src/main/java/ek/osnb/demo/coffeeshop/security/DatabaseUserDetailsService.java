package ek.osnb.demo.coffeeshop.security;

import ek.osnb.demo.coffeeshop.account.AccountApi;
import ek.osnb.demo.coffeeshop.account.AccountInfo;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountApi accounts;

    DatabaseUserDetailsService(AccountApi accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AccountInfo account = accounts.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));

        return User.withUsername(account.email())
                .password(account.passwordHash())
                .authorities(account.roles().toArray(String[]::new))
                .build();
    }
}
