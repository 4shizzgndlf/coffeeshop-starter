// components/CartBadge.js

import { getCurrentCart, subscribeToCart } from "../cart.js";

// The number of items in the cart, in the <span class="cart-badge"> next to "Cart"
// in the header (index.html). Redraws itself whenever cart.js says the cart changed,
// so pages never have to update it themselves.
export function initCartBadge() {
    const badge = document.querySelector("#cart-link .cart-badge");

    function renderBadge() {
        const count = getCurrentCart()?.totalQuantity ?? 0;

        badge.textContent = String(count);
        badge.hidden = count === 0;
    }

    renderBadge();
    subscribeToCart(renderBadge);
}
