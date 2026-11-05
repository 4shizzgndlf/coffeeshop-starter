// components/UserMenu.js

import { getCurrentUser, hasRole, refreshUser, subscribeToUser } from "../auth.js";
import { router } from "../router.js";
import { logout } from "../services/auth.js";

// The right end of the header: "Log in", or the user's email and "Log out".
// Also shows the Staff link only to staff. Redraws itself whenever auth.js says the
// user changed, so pages never have to update the header.
//
// Hiding a link is not authorization: Spring Security still protects /api/staff/**.
export function initUserMenu() {
    const menu = document.querySelector("#user-menu");
    const staffLink = document.querySelector("#staff-link");

    function renderMenu(user) {
        staffLink.hidden = !hasRole("ROLE_STAFF");

        if (!user) {
            menu.innerHTML = `<a href="/login" data-navigo>Log in</a>`;
            return;
        }

        menu.innerHTML = `
            <span class="user-menu__name"></span>
            <button type="button" class="user-menu__logout">Log out</button>
        `;

        menu.querySelector(".user-menu__name").textContent = user.username;

        menu.querySelector(".user-menu__logout").addEventListener("click", async () => {
            try {
                await logout();
                // Spring deleted the session and the CSRF token; this gets a new token
                await refreshUser();
                router.navigate("/");
            } catch (error) {
                console.error("Logout failed:", error);
            }
        });
    }

    renderMenu(getCurrentUser());
    subscribeToUser(renderMenu);
}
