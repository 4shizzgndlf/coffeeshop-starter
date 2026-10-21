// pages/cartPage.js

import { changeQuantity, removeFromCart } from "../api/carts.js";
import { createCartLine } from "../components/CartLine.js";
import { createCartSummary } from "../components/CartSummary.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { loadCart } from "../session.js";

export async function renderCartPage(container) {
    container.textContent = "Loading...";

    let cart;

    try {
        cart = await loadCart();
    } catch (error) {
        container.textContent = `Could not load the cart. ${error.message}`;
        return;
    }

    const heading = document.createElement("h1");
    heading.textContent = "Your cart";

    const errorMessage = createErrorMessage();

    let content = document.createElement("div");

    // Rebuilds everything below the heading from `cart` - called after every change
    function renderCart() {
        const newContent = !cart || cart.lines.length === 0 ? createEmptyCart() : createFilledCart();
        content.replaceWith(newContent);
        content = newContent;
    }

    function createEmptyCart() {
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.innerHTML = `Your cart is empty. <a href="/" data-navigo>Browse the shop</a>`;
        return empty;
    }

    function createFilledCart() {
        const layout = document.createElement("div");
        layout.className = "layout-with-sidebar";

        const list = document.createElement("ul");
        list.className = "cart-lines";

        for (const line of cart.lines) {
            list.append(createCartLine({
                line,
                onQuantityChange: changeLineQuantity,
                onRemove: removeLine
            }));
        }

        layout.append(list, createCartSummary({ cart, checkoutLink: true }));

        return layout;
    }

    // The backend answers with the whole updated cart, so we just draw that
    async function changeLineQuantity(line, quantity) {
        try {
            cart = await changeQuantity(cart.id, line.productId, quantity);
            errorMessage.hide();
        } catch (error) {
            errorMessage.show(error.message);
        }

        // Also puts the old quantity back if saving failed
        renderCart();
    }

    async function removeLine(line) {
        try {
            cart = await removeFromCart(cart.id, line.productId);
            errorMessage.hide();
        } catch (error) {
            errorMessage.show(error.message);
        }

        renderCart();
    }

    container.replaceChildren(heading, errorMessage.element, content);
    renderCart();
}
