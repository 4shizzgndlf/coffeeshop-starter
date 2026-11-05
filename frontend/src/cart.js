// cart.js
//
// The cart of this browser, shared by the pages and the badge in the header.
// Every change goes through here, so subscribeToCart() listeners (like the badge)
// always hear about it and no page has to update them itself.

import { addCartItem, createCart, getCart, removeCartItem, updateCartItem } from "./services/carts.js";
import { forgetCartId, getCartId, setCartId } from "./session.js";

let cart = null;

const listeners = new Set();

function notify() {
    listeners.forEach(listener => listener());
}

// The backend answers every change with the whole updated cart, so we just keep that
function setCart(newCart) {
    cart = newCart;
    notify();
    return cart;
}

// The cart as it was last loaded, or null if there is none yet
export function getCurrentCart() {
    return cart;
}

// Lets other parts of the app know when the cart changes
export function subscribeToCart(listener) {
    listeners.add(listener);
}

// Loads the cart again, with today's prices and stock. Resolves to null if there is no cart yet.
export async function refreshCart() {
    const id = getCartId();

    if (id === null) {
        return setCart(null);
    }

    try {
        return setCart(await getCart(id));
    } catch (error) {
        // The cart no longer exists, e.g. after "docker compose down -v": forget it
        if (error.status === 404) {
            forgetCartId();
            return setCart(null);
        }

        throw error;
    }
}

// Creates the cart on the first "Add to cart"
export async function addToCart(productId, quantity) {
    let current = await refreshCart();

    if (!current) {
        current = await createCart();
        setCartId(current.id);
    }

    return setCart(await addCartItem(current.id, productId, quantity));
}

export async function changeQuantity(productId, quantity) {
    return setCart(await updateCartItem(cart.id, productId, quantity));
}

export async function removeFromCart(productId) {
    return setCart(await removeCartItem(cart.id, productId));
}
