package ek.osnb.demo.coffeeshop.cart;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Every change returns the whole updated cart, so the frontend can redraw it
// without a second request.
@RestController
@RequestMapping("/api/carts")
class CartController {

    private final CartService service;

    CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CartResponse create() {
        return service.create();
    }

    @GetMapping("/{id}")
    CartResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/items")
    CartResponse addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddCartItemRequest request) {

        return service.addItem(id, request);
    }

    @PutMapping("/{id}/items/{productId}")
    CartResponse changeQuantity(
            @PathVariable Long id,
            @PathVariable Long productId,
            @Valid @RequestBody ChangeQuantityRequest request) {

        return service.changeQuantity(id, productId, request);
    }

    @DeleteMapping("/{id}/items/{productId}")
    CartResponse removeItem(
            @PathVariable Long id,
            @PathVariable Long productId) {

        return service.removeItem(id, productId);
    }
}
