package ek.osnb.demo.coffeeshop.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates one local staff account when STAFF_EMAIL and STAFF_PASSWORD are configured. */
@Component
class StaffAccountInitializer implements ApplicationRunner {

    private final AccountService accounts;
    private final String email;
    private final String password;

    StaffAccountInitializer(
            AccountService accounts,
            @Value("${staff.seed.email:}") String email,
            @Value("${staff.seed.password:}") String password) {
        this.accounts = accounts;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        accounts.createStaffIfMissing(email, password);
    }
}
