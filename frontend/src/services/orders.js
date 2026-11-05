// services/orders.js - placing and viewing orders

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/orders";

// Turns the cart into an order for the logged-in customer. The backend copies names,
// prices and the delivery address, takes the items out of stock and empties the cart.
export function placeOrder({ cartId }) {
    return sendJSON("POST", BASE_URL, { cartId });
}

// The logged-in customer's orders, newest first
export function getMyOrders() {
    return getJSON(BASE_URL);
}

export function getOrder(id) {
    return getJSON(`${BASE_URL}/${id}`);
}

export function cancelOrder(id) {
    return sendJSON("POST", `${BASE_URL}/${id}/cancel`);
}

// ---------- Staff only ----------

// Every customer's orders, newest first
export function getAllOrders() {
    return getJSON("/api/staff/orders");
}

export function shipOrder(id) {
    return sendJSON("POST", `${BASE_URL}/${id}/ship`);
}
