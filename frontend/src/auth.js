// auth.js
//
// Who is logged in, shared by the pages, the guards and the user menu in the header.
// An SPA forgets everything when the page reloads, but the session cookie survives,
// so app.js asks the backend once at startup (refreshUser), and again after logging in or out.

import { getMe } from "./services/auth.js";

let currentUser = null;

// false while the backend has no GET /api/me yet (it answers 404): the login isn't built
let loginAvailable = true;

const listeners = new Set();

// { username, roles: ["ROLE_CUSTOMER"] }, or null if nobody is logged in
export function getCurrentUser() {
    return currentUser;
}

export function setCurrentUser(user) {
    currentUser = user;
    listeners.forEach(listener => listener(user));
}

// Lets other parts of the app know when the user changes
export function subscribeToUser(listener) {
    listeners.add(listener);
}

// e.g. hasRole("ROLE_STAFF")
export function hasRole(role) {
    return currentUser?.roles.includes(role) ?? false;
}

export function isLoginAvailable() {
    return loginAvailable;
}

export async function refreshUser() {
    try {
        const user = await getMe();
        loginAvailable = true;
        setCurrentUser(user);
    } catch (error) {
        if (error.status === 404) {
            loginAvailable = false;
            setCurrentUser(null);
            return;
        }

        throw error;
    }
}
