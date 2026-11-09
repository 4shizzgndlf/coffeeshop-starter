package ek.osnb.demo.coffeeshop.account;

import java.util.Optional;

public interface AccountApi {
    Optional<AccountInfo> findByEmail(String email);
}
