package ek.osnb.demo.coffeeshop.cart;

import org.springframework.data.jpa.repository.JpaRepository;

interface CartRepository extends JpaRepository<Cart, Long> {
}
