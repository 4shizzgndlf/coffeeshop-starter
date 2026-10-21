package ek.osnb.demo.coffeeshop.cart;

import ek.osnb.demo.coffeeshop.catalog.CatalogApi;
import ek.osnb.demo.coffeeshop.catalog.InsufficientStockException;
import ek.osnb.demo.coffeeshop.catalog.ProductInfo;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The cart only stores product ids. Everything else about a product
 * (does it exist, its name, price and stock) is asked through CatalogApi.
 */
@Service
@Transactional
class CartService implements CartApi {

    private final CartRepository repository;
    private final CatalogApi catalog;

    CartService(CartRepository repository, CatalogApi catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    // ---------- Used by CartController ----------

    CartResponse create() {
        Cart cart = repository.save(Cart.create(Instant.now()));
        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    CartResponse findById(Long id) {
        return toResponse(findCart(id));
    }

    CartResponse addItem(Long cartId, AddCartItemRequest request) {
        Cart cart = findCart(cartId);

        // Throws NotFoundException (404) if the product doesn't exist
        ProductInfo product = catalog.getProduct(request.productId());

        int wanted = cart.quantityOf(product.id()) + request.quantity();
        requireInStock(product, wanted);

        cart.addItem(product.id(), request.quantity());

        return toResponse(cart);
    }

    CartResponse changeQuantity(Long cartId, Long productId, ChangeQuantityRequest request) {
        Cart cart = findCart(cartId);
        ProductInfo product = catalog.getProduct(productId);

        requireInStock(product, request.quantity());
        cart.changeQuantity(productId, request.quantity());

        return toResponse(cart);
    }

    CartResponse removeItem(Long cartId, Long productId) {
        Cart cart = findCart(cartId);
        cart.removeItem(productId);

        return toResponse(cart);
    }

    // ---------- CartApi: used by other features ----------

    @Override
    @Transactional(readOnly = true)
    public CartInfo getCart(Long cartId) {
        Cart cart = findCart(cartId);
        List<CartLineInfo> lines = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            lines.add(new CartLineInfo(item.getProductId(), item.getQuantity()));
        }

        return new CartInfo(cart.getId(), lines);
    }

    @Override
    public void clearCart(Long cartId) {
        findCart(cartId).clear();
    }

    // ---------- Helpers ----------

    // The stock is checked again when the order is placed: someone else
    // may buy the last bag between "add to cart" and "place order".
    private static void requireInStock(ProductInfo product, int wanted) {
        if (wanted > product.stock()) {
            throw new InsufficientStockException(product.name(), product.stock());
        }
    }

    // Builds the response with live names and prices from the catalog.
    // One findProducts() call for the whole cart, not one call per item.
    private CartResponse toResponse(Cart cart) {
        List<Long> productIds = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            productIds.add(item.getProductId());
        }

        Map<Long, ProductInfo> products = catalog.findProducts(productIds);

        List<CartLineResponse> lines = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            ProductInfo product = products.get(item.getProductId());

            // The catalog no longer has this product: show it, but it can't be bought
            String name = product == null ? "Unavailable product" : product.name();
            BigDecimal unitPrice = product == null ? BigDecimal.ZERO : product.price();
            int available = product == null ? 0 : product.stock();

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            lines.add(new CartLineResponse(
                    item.getProductId(), name, unitPrice, item.getQuantity(), lineTotal, available));

            totalQuantity += item.getQuantity();
            total = total.add(lineTotal);
        }

        return new CartResponse(cart.getId(), lines, totalQuantity, total);
    }

    private Cart findCart(Long id) {
        Optional<Cart> result = repository.findById(id);

        if (result.isEmpty()) {
            throw new NotFoundException("Cart not found: " + id);
        }

        return result.get();
    }
}
