// services/customers.js - customer profiles

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/customers";

// customer: { name, email, street, postalCode, city }
export function createCustomer(customer) {
    return sendJSON("POST", BASE_URL, customer);
}

export function getCustomer(id) {
    return getJSON(`${BASE_URL}/${id}`);
}

export function updateCustomer(id, customer) {
    return sendJSON("PUT", `${BASE_URL}/${id}`, customer);
}

// Resolves to the customer with this email, or null if there is none.
// A stand-in for logging in until the app has Spring Security.
export async function findCustomerByEmail(email) {
    const customers = await getJSON(`${BASE_URL}?email=${encodeURIComponent(email)}`);
    return customers[0] ?? null;
}
