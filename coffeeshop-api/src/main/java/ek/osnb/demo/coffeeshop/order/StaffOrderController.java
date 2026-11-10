package ek.osnb.demo.coffeeshop.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/orders")
class StaffOrderController {

    private final OrderService orders;

    StaffOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    List<OrderSummaryResponse> findAll() {
        return orders.findAll();
    }
}
