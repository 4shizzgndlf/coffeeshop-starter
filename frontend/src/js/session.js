// session.js
//
// Remembers who is shopping between page loads, in localStorage:
// - the cart id, created on the first "Add to cart"
// - the customer id, created at checkout or on the profile page
//
// There is no login yet. With Spring Security, the customer would come from the
// login instead, and the cart would belong to the logged-in customer.

import { createCart, getCart, notifyCartChanged } from "./api/carts.js";
import { getCustomer } from "./api/customers.js";

const CART_KEY = "coffeeshop.cartId";
const CUSTOMER_KEY = "coffeeshop.customerId";

// ---------- Cart ----------

// The cart of this browser, or null if there is none yet. Does not create one.
export async function loadCart() {
    const id = read(CART_KEY);

    if (id === null) {
        return null;
    }

    try {
        const cart = await getCart(id);
        notifyCartChanged(cart);
        return cart;
    } catch (error) {
        // The cart no longer exists, e.g. after "docker compose down -v": forget it
        if (error.status === 404) {
            remove(CART_KEY);
            return null;
        }

        throw error;
    }
}

// The id of this browser's cart. Creates a new cart the first time.
export async function ensureCartId() {
    const cart = await loadCart();

    if (cart) {
        return cart.id;
    }

    const created = await createCart();
    write(CART_KEY, created.id);

    return created.id;
}

// ---------- Customer ----------

// The customer of this browser, or null if there is none yet
export async function loadCustomer() {
    const id = read(CUSTOMER_KEY);

    if (id === null) {
        return null;
    }

    try {
        return await getCustomer(id);
    } catch (error) {
        // The customer no longer exists, e.g. after "docker compose down -v": forget it
        if (error.status === 404) {
            remove(CUSTOMER_KEY);
            return null;
        }

        throw error;
    }
}

export function getCustomerId() {
    return read(CUSTOMER_KEY);
}

export function setCustomerId(id) {
    write(CUSTOMER_KEY, id);
}

export function forgetCustomer() {
    remove(CUSTOMER_KEY);
}

// ---------- localStorage ----------
// Wrapped in try/catch: localStorage can be blocked, e.g. in some private windows.
// The shop still works then, it just forgets the cart on reload.

function read(key) {
    try {
        const value = localStorage.getItem(key);
        return value === null ? null : Number(value);
    } catch (error) {
        return null;
    }
}

function write(key, value) {
    try {
        localStorage.setItem(key, String(value));
    } catch (error) {
        // Ignore: see above
    }
}

function remove(key) {
    try {
        localStorage.removeItem(key);
    } catch (error) {
        // Ignore: see above
    }
}
