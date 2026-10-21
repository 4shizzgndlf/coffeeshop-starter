package ek.osnb.demo.coffeeshop.order;

import ek.osnb.demo.coffeeshop.cart.CartApi;
import ek.osnb.demo.coffeeshop.cart.CartInfo;
import ek.osnb.demo.coffeeshop.cart.CartLineInfo;
import ek.osnb.demo.coffeeshop.catalog.CatalogApi;
import ek.osnb.demo.coffeeshop.catalog.ProductInfo;
import ek.osnb.demo.coffeeshop.common.ConflictException;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import ek.osnb.demo.coffeeshop.customer.CustomerApi;
import ek.osnb.demo.coffeeshop.customer.CustomerInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orders use three other features, always through their public *Api interface:
 * CustomerApi (who is ordering), CartApi (what is ordered) and CatalogApi (names,
 * prices and stock). The order itself only stores their ids, plus copies of the
 * values that must never change afterwards.
 */
@Service
@Transactional
class OrderService {

    private final OrderRepository repository;
    private final CustomerApi customers;
    private final CartApi carts;
    private final CatalogApi catalog;
    private final ApplicationEventPublisher events;

    OrderService(
            OrderRepository repository,
            CustomerApi customers,
            CartApi carts,
            CatalogApi catalog,
            ApplicationEventPublisher events) {

        this.repository = repository;
        this.customers = customers;
        this.carts = carts;
        this.catalog = catalog;
        this.events = events;
    }

    // Everything in here happens in one transaction: if any step fails (e.g. one
    // product is out of stock), nothing is saved - no stock is taken, no order is
    // created and the cart keeps its items.
    OrderResponse placeOrder(PlaceOrderRequest request) {
        CustomerInfo customer = customers.getCustomer(request.customerId());
        CartInfo cart = carts.getCart(request.cartId());

        if (cart.isEmpty()) {
            throw new ConflictException("The cart is empty");
        }

        List<Long> productIds = new ArrayList<>();
        for (CartLineInfo line : cart.lines()) {
            productIds.add(line.productId());
        }
        Map<Long, ProductInfo> products = catalog.findProducts(productIds);

        List<OrderLine> lines = new ArrayList<>();

        for (CartLineInfo cartLine : cart.lines()) {
            ProductInfo product = products.get(cartLine.productId());

            if (product == null) {
                throw new NotFoundException("Product not found: " + cartLine.productId());
            }

            // Throws InsufficientStockException (409) if there are too few left
            catalog.reserveStock(product.id(), cartLine.quantity());

            // Copy the name and price: the order keeps them even if the catalog changes
            lines.add(new OrderLine(product.id(), product.name(), product.price(), cartLine.quantity()));
        }

        ShippingAddress address = new ShippingAddress(
                customer.name(), customer.street(), customer.postalCode(), customer.city());

        Order order = repository.save(Order.place(customer.id(), address, lines, Instant.now()));

        carts.clearCart(cart.id());

        // Lets other features react, without the order feature knowing about them
        events.publishEvent(new OrderPlacedEvent(
                order.getId(), order.getCustomerId(), order.getTotal(), order.getPlacedAt()));

        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    List<OrderSummaryResponse> findByCustomer(Long customerId) {
        // 404 for an unknown customer instead of an empty list
        customers.getCustomer(customerId);

        List<OrderSummaryResponse> summaries = new ArrayList<>();

        for (Order order : repository.findByCustomerIdOrderByPlacedAtDesc(customerId)) {
            summaries.add(OrderMapper.toSummary(order));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    OrderResponse findById(Long id) {
        return OrderMapper.toResponse(findOrder(id));
    }

    OrderResponse cancel(Long id) {
        Order order = findOrder(id);
        order.cancel();

        // The items go back on the shelf
        for (OrderLine line : order.getLines()) {
            catalog.releaseStock(line.getProductId(), line.getQuantity());
        }

        return OrderMapper.toResponse(order);
    }

    OrderResponse ship(Long id) {
        Order order = findOrder(id);
        order.ship();

        return OrderMapper.toResponse(order);
    }

    private Order findOrder(Long id) {
        Optional<Order> result = repository.findById(id);

        if (result.isEmpty()) {
            throw new NotFoundException("Order not found: " + id);
        }

        return result.get();
    }
}
