package ek.osnb.demo.coffeeshop.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OrderRepository extends JpaRepository<Order, Long> {

    // Loads the lines in the same query: the summary counts the items of each order
    @EntityGraph(attributePaths = "lines")
    List<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId);
}
