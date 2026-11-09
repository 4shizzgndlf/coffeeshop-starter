package ek.osnb.demo.coffeeshop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/register", "/api/login", "/api/products/**", "/api/categories/**", "/api/carts/**").permitAll()
                        .requestMatchers("/api/staff/**").hasRole("STAFF")
                        .requestMatchers("/api/me", "/api/customers/me/**", "/api/orders/**", "/api/assistant/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
