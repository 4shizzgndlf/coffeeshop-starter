// api/carts.js - the shopping cart
//
// Every change returns the whole updated cart and announces it with a
// "cart:changed" event, so e.g. the badge in the header can update itself
// without the page knowing about it.

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/carts";

export function createCart() {
    return sendJSON("POST", BASE_URL);
}

export function getCart(id) {
    return getJSON(`${BASE_URL}/${id}`);
}

export async function addToCart(cartId, productId, quantity) {
    const cart = await sendJSON("POST", `${BASE_URL}/${cartId}/items`, { productId, quantity });
    notifyCartChanged(cart);
    return cart;
}

export async function changeQuantity(cartId, productId, quantity) {
    const cart = await sendJSON("PUT", `${BASE_URL}/${cartId}/items/${productId}`, { quantity });
    notifyCartChanged(cart);
    return cart;
}

export async function removeFromCart(cartId, productId) {
    const cart = await sendJSON("DELETE", `${BASE_URL}/${cartId}/items/${productId}`);
    notifyCartChanged(cart);
    return cart;
}

// Anyone can listen: document.addEventListener("cart:changed", (event) => event.detail.totalQuantity)
export function notifyCartChanged(cart) {
    document.dispatchEvent(new CustomEvent("cart:changed", { detail: cart }));
}
