// services/orders.js - placing and viewing orders

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/orders";

// Turns the cart into an order. The backend copies names, prices and the
// address, takes the items out of stock and empties the cart.
export function placeOrder({ customerId, cartId }) {
    return sendJSON("POST", BASE_URL, { customerId, cartId });
}

// Newest first
export function getOrders(customerId) {
    return getJSON(`${BASE_URL}?customerId=${customerId}`);
}

export function getOrder(id) {
    return getJSON(`${BASE_URL}/${id}`);
}

export function cancelOrder(id) {
    return sendJSON("POST", `${BASE_URL}/${id}/cancel`);
}
