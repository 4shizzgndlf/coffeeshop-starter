package ek.osnb.demo.coffeeshop.account;

import java.util.Set;

public record AccountInfo(String email, String passwordHash, Set<String> roles) {
}
