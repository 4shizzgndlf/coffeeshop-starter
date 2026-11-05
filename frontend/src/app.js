import { refreshCart } from "./cart.js";
import { initCartBadge } from "./components/CartBadge.js";
import { CartPage } from "./pages/CartPage.js";
import { CheckoutPage } from "./pages/CheckoutPage.js";
import { NotFoundPage, ServerDownPage } from "./pages/ErrorPages.js";
import { OrderPage } from "./pages/OrderPage.js";
import { OrdersPage } from "./pages/OrdersPage.js";
import { ProductPage } from "./pages/ProductPage.js";
import { ProductsPage } from "./pages/ProductsPage.js";
import { ProfilePage } from "./pages/ProfilePage.js";
import { render, router } from "./router.js";

async function init() {
    // The item count next to "Cart" in the header
    initCartBadge();

    // Loads the cart from an earlier visit, so the badge shows its count. This only calls
    // the API when there is a stored cart: a first visit with the API down gets the
    // page's own error message instead.
    try {
        await refreshCart();
    } catch (error) {
        console.error(error);
        render(ServerDownPage, { message: error.message });
        return;
    }

    // Routes: path -> page. Add new pages here.
    router
        .on("/", () => render(ProductsPage))
        .on("/products/:id", ({ data }) => render(ProductPage, { id: data.id }))
        .on("/cart", () => render(CartPage))
        .on("/checkout", () => render(CheckoutPage))
        .on("/orders", () => render(OrdersPage))
        .on("/orders/:id", ({ data }) => render(OrderPage, { id: data.id }))
        .on("/profile", () => render(ProfilePage))
        .notFound(() => render(NotFoundPage))
        .resolve();
}

init();
