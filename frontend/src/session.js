// session.js
//
// Remembers this browser's cart between page loads, in localStorage. The cart id is
// created on the first "Add to cart" (the cart itself is in cart.js). Anyone can
// shop without logging in; the login is only needed at checkout.
//
// Who is logged in is not stored here: the session cookie remembers that, and
// auth.js asks the backend (GET /api/me).

const CART_KEY = "coffeeshop.cartId";

export function getCartId() {
    return read(CART_KEY);
}

export function setCartId(id) {
    write(CART_KEY, id);
}

export function forgetCartId() {
    remove(CART_KEY);
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
