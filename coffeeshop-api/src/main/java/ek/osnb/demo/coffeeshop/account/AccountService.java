package ek.osnb.demo.coffeeshop.account;

import ek.osnb.demo.coffeeshop.common.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
class AccountService implements AccountApi {

    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    AccountService(AccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        repository.save(Account.customer(email, passwordEncoder.encode(request.password())));
    }

    void createStaffIfMissing(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!repository.existsByEmail(normalizedEmail)) {
            repository.save(Account.staff(normalizedEmail, passwordEncoder.encode(password)));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountInfo> findByEmail(String email) {
        return repository.findByEmail(email.trim().toLowerCase())
                .map(account -> new AccountInfo(
                        account.getEmail(),
                        account.getPasswordHash(),
                        account.getRoles().stream().map(role -> "ROLE_" + role.name()).collect(java.util.stream.Collectors.toUnmodifiableSet())
                ));
    }
}
