import { router, page } from "./router.js";
import { createCartBadge } from "./components/CartBadge.js";
import { loadCart } from "./session.js";
import { renderProductsPage } from "./pages/productsPage.js";
import { renderProductPage } from "./pages/productPage.js";
import { renderCartPage } from "./pages/cartPage.js";
import { renderCheckoutPage } from "./pages/checkoutPage.js";
import { renderOrdersPage } from "./pages/ordersPage.js";
import { renderOrderPage } from "./pages/orderPage.js";
import { renderProfilePage } from "./pages/profilePage.js";
import { renderNotFoundPage } from "./pages/notFoundPage.js";

// The item count next to "Cart" in the header. It updates itself on every
// "cart:changed" event, so no page has to remember to update it.
document.querySelector("#cart-link").append(createCartBadge().element);

// Routes: path -> page. Add new pages here.
router
    .on("/", page(renderProductsPage, "Shop"))
    .on("/products/:id", page(renderProductPage, "Product"))
    .on("/cart", page(renderCartPage, "Cart"))
    .on("/checkout", page(renderCheckoutPage, "Checkout"))
    .on("/orders", page(renderOrdersPage, "My orders"))
    .on("/orders/:id", page(renderOrderPage, "Order"))
    .on("/profile", page(renderProfilePage, "Profile"))
    .notFound(page(renderNotFoundPage, "Not found"))
    .resolve();

// Shows the item count of a cart from an earlier visit. If it fails (e.g. the
// API is down), the badge simply stays hidden - the page shows its own error.
loadCart().catch(() => {});
