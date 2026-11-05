// services/carts.js - the shopping cart
//
// Every change returns the whole updated cart. Pages don't call these directly:
// cart.js does, so it can tell the badge in the header about the change.

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/carts";

export function createCart() {
    return sendJSON("POST", BASE_URL);
}

export function getCart(id) {
    return getJSON(`${BASE_URL}/${id}`);
}

export function addCartItem(cartId, productId, quantity) {
    return sendJSON("POST", `${BASE_URL}/${cartId}/items`, { productId, quantity });
}

export function updateCartItem(cartId, productId, quantity) {
    return sendJSON("PUT", `${BASE_URL}/${cartId}/items/${productId}`, { quantity });
}

export function removeCartItem(cartId, productId) {
    return sendJSON("DELETE", `${BASE_URL}/${cartId}/items/${productId}`);
}
