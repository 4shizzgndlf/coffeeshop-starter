// components/CartBadge.js

// The number of items in the cart, shown next to "Cart" in the header.
// Updates itself on every "cart:changed" event (see notifyCartChanged in
// api/carts.js), so pages never have to update it themselves.
export function createCartBadge() {
    const element = document.createElement("span");

    element.className = "cart-badge";
    element.hidden = true;

    document.addEventListener("cart:changed", (event) => {
        const count = event.detail?.totalQuantity ?? 0;

        element.textContent = String(count);
        element.hidden = count === 0;
    });

    return {
        element
    };
}
