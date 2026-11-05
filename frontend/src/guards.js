// guards.js
//
// A guard runs before a page is rendered and decides whether to show it.
// Use them in app.js: .on("/orders", requireRole(OrdersPage, "ROLE_CUSTOMER"))
//
// Guards only decide what the user sees. Anyone can change the JavaScript in
// their own browser, so Spring Security must still protect every endpoint.

import { getCurrentUser, setCurrentUser } from "./auth.js";
import { ForbiddenPage } from "./pages/ErrorPages.js";
import { render, router } from "./router.js";

// Replace the guarded URL, so the Back button doesn't lead straight back to the redirect
const REDIRECT = { historyAPIMethod: "replaceState" };

// Where the user wanted to go before being sent to /login, e.g. "/checkout"
let returnPath = null;

// The page gets the route's parameters as props, e.g. { id: "3" } for /orders/:id
export function requireAuth(Page) {
    return (match) => {
        if (!getCurrentUser()) {
            goToLogin(`/${match.url}`);
            return;
        }

        render(Page, match.data ?? {});
    };
}

export function requireRole(Page, role) {
    return (match) => {
        const user = getCurrentUser();

        if (!user) {
            goToLogin(`/${match.url}`);
            return;
        }

        if (!user.roles.includes(role)) {
            render(ForbiddenPage);
            return;
        }

        render(Page, match.data ?? {});
    };
}

// Keeps users who are already logged in away from the login and register forms
export function requireGuest(Page) {
    return () => {
        if (getCurrentUser()) {
            router.navigate("/", REDIRECT);
            return;
        }

        render(Page);
    };
}

// A 401 from the API means the session is gone, e.g. after a server restart.
// Pages call this when a request fails. It returns true if it took care of the error.
export function handleAuthError(error) {
    if (error.status !== 401) {
        return false;
    }

    setCurrentUser(null);
    goToLogin(window.location.pathname);
    return true;
}

// Used by the login page: where to go after logging in. Asks only once.
export function takeReturnPath(fallback = "/") {
    const path = returnPath ?? fallback;
    returnPath = null;
    return path;
}

function goToLogin(path) {
    returnPath = path;
    router.navigate("/login", REDIRECT);
}
