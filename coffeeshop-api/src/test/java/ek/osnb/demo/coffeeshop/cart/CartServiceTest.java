package ek.osnb.demo.coffeeshop.cart;

import ek.osnb.demo.coffeeshop.catalog.CatalogApi;
import ek.osnb.demo.coffeeshop.catalog.InsufficientStockException;
import ek.osnb.demo.coffeeshop.catalog.ProductInfo;
import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests CartService with a mocked CatalogApi: the cart only knows products by id,
 * so a fake catalog is all it needs. No Spring, no database, no Docker.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long CART_ID = 1L;
    private static final ProductInfo BEANS = new ProductInfo(10L, "Colombia Huila", new BigDecimal("99.00"), 50);

    @Mock
    private CartRepository repository;

    @Mock
    private CatalogApi catalog;

    @InjectMocks
    private CartService service;

    private final Cart cart = Cart.create(Instant.now());

    @Test
    void addItemShowsTheLiveNameAndPriceFromTheCatalog() {
        when(repository.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(catalog.getProduct(BEANS.id())).thenReturn(BEANS);
        when(catalog.findProducts(List.of(BEANS.id()))).thenReturn(Map.of(BEANS.id(), BEANS));

        CartResponse response = service.addItem(CART_ID, new AddCartItemRequest(BEANS.id(), 2));

        CartLineResponse line = response.lines().get(0);
        assertEquals("Colombia Huila", line.productName());
        assertEquals(new BigDecimal("99.00"), line.unitPrice());
        assertEquals(new BigDecimal("198.00"), line.lineTotal());
        assertEquals(2, response.totalQuantity());
        assertEquals(new BigDecimal("198.00"), response.total());
    }

    @Test
    void addingMoreThanInStockIsRejectedAndKeepsTheCart() {
        cart.addItem(BEANS.id(), 45);
        when(repository.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(catalog.getProduct(BEANS.id())).thenReturn(BEANS);

        // 45 already in the cart + 10 more > 50 in stock
        assertThrows(InsufficientStockException.class,
                () -> service.addItem(CART_ID, new AddCartItemRequest(BEANS.id(), 10)));

        assertEquals(45, cart.quantityOf(BEANS.id()));
    }

    @Test
    void addingAnUnknownProductIsRejected() {
        when(repository.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(catalog.getProduct(99L)).thenThrow(new NotFoundException("Product not found: 99"));

        assertThrows(NotFoundException.class,
                () -> service.addItem(CART_ID, new AddCartItemRequest(99L, 1)));

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void productThatLeftTheCatalogIsShownAsUnavailable() {
        cart.addItem(BEANS.id(), 1);
        when(repository.findById(CART_ID)).thenReturn(Optional.of(cart));
        when(catalog.findProducts(List.of(BEANS.id()))).thenReturn(Map.of());

        CartResponse response = service.findById(CART_ID);

        assertEquals("Unavailable product", response.lines().get(0).productName());
        assertEquals(0, response.lines().get(0).available());
        assertEquals(BigDecimal.ZERO, response.total());
    }

    @Test
    void getCartGivesOtherFeaturesIdsAndQuantitiesOnly() {
        cart.addItem(BEANS.id(), 3);
        when(repository.findById(CART_ID)).thenReturn(Optional.of(cart));

        CartInfo info = service.getCart(CART_ID);

        assertEquals(List.of(new CartLineInfo(BEANS.id(), 3)), info.lines());
    }
}
