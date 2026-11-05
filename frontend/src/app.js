import { refreshUser } from "./auth.js";
import { refreshCart } from "./cart.js";
import { initCartBadge } from "./components/CartBadge.js";
import { initUserMenu } from "./components/UserMenu.js";
import { requireAuth, requireGuest, requireRole } from "./guards.js";
import { AssistantPage } from "./pages/AssistantPage.js";
import { CartPage } from "./pages/CartPage.js";
import { CheckoutPage } from "./pages/CheckoutPage.js";
import { NotFoundPage, ServerDownPage } from "./pages/ErrorPages.js";
import { LoginPage } from "./pages/LoginPage.js";
import { OrderPage } from "./pages/OrderPage.js";
import { OrdersPage } from "./pages/OrdersPage.js";
import { ProductPage } from "./pages/ProductPage.js";
import { ProductsPage } from "./pages/ProductsPage.js";
import { ProfilePage } from "./pages/ProfilePage.js";
import { RegisterPage } from "./pages/RegisterPage.js";
import { StaffOrdersPage } from "./pages/StaffOrdersPage.js";
import { render, router } from "./router.js";

async function init() {
    // The header: the item count next to "Cart", and "Log in" or the user's email
    initCartBadge();
    initUserMenu();

    // Asks the backend who is logged in (GET /api/me), and loads the cart from an
    // earlier visit. The first request also gives the browser its CSRF token cookie.
    try {
        await Promise.all([refreshUser(), refreshCart()]);
    } catch (error) {
        console.error(error);
        render(ServerDownPage, { message: error.message });
        return;
    }

    // Routes: path -> page. Add new pages here.
    // The guards (guards.js) send guests to /login and check roles before a page is shown.
    router
        // Anyone, logged in or not
        .on("/", () => render(ProductsPage))
        .on("/products/:id", ({ data }) => render(ProductPage, { id: data.id }))
        .on("/cart", () => render(CartPage))
        .on("/login", requireGuest(LoginPage))
        .on("/register", requireGuest(RegisterPage))
        // Customers
        .on("/checkout", requireRole(CheckoutPage, "ROLE_CUSTOMER"))
        .on("/orders", requireRole(OrdersPage, "ROLE_CUSTOMER"))
        .on("/profile", requireRole(ProfilePage, "ROLE_CUSTOMER"))
        // Customers (their own orders) and staff (every order)
        .on("/orders/:id", requireAuth(OrderPage))
        .on("/assistant", requireAuth(AssistantPage))
        // Staff
        .on("/staff", requireRole(StaffOrdersPage, "ROLE_STAFF"))
        .notFound(() => render(NotFoundPage))
        .resolve();
}

init();
