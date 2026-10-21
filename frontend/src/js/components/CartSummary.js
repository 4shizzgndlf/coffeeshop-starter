// components/CartSummary.js

import { formatPrice } from "../format.js";

// The totals of a cart. showLines lists each product (used on the checkout page),
// checkoutLink adds a "Go to checkout" button (used on the cart page).
export function createCartSummary({
    cart,
    showLines = false,
    checkoutLink = false
}) {
    const element = document.createElement("aside");

    element.className = "cart-summary";

    element.innerHTML = `
        <h2 class="cart-summary__title">Order summary</h2>
        <ul class="cart-summary__lines"></ul>
        <dl class="cart-summary__rows">
            <div><dt>Items</dt><dd class="cart-summary__count"></dd></div>
            <div><dt>Shipping</dt><dd>Free</dd></div>
            <div class="cart-summary__total"><dt>Total</dt><dd class="cart-summary__amount"></dd></div>
        </dl>
      `;

    const lines = element.querySelector(".cart-summary__lines");

    if (showLines) {
        for (const line of cart.lines) {
            const item = document.createElement("li");
            const name = document.createElement("span");
            const total = document.createElement("span");

            name.textContent = `${line.quantity} × ${line.productName}`;
            total.textContent = formatPrice(line.lineTotal);

            item.append(name, total);
            lines.append(item);
        }
    } else {
        lines.remove();
    }

    element.querySelector(".cart-summary__count").textContent = String(cart.totalQuantity);
    element.querySelector(".cart-summary__amount").textContent = formatPrice(cart.total);

    if (checkoutLink) {
        // A link styled as a button (button.css): navigating is what links are for
        const link = document.createElement("a");
        link.className = "button";
        link.dataset.variant = "primary";
        link.dataset.size = "large";
        link.setAttribute("href", "/checkout");
        link.setAttribute("data-navigo", "");
        link.textContent = "Go to checkout";
        element.append(link);
    }

    return element;
}
