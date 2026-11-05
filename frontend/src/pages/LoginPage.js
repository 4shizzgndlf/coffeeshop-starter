// pages/LoginPage.js

import { isLoginAvailable, refreshUser } from "../auth.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createNotBuiltNotice } from "../components/NotBuiltNotice.js";
import { takeReturnPath } from "../guards.js";
import { router, setTitle } from "../router.js";
import { login } from "../services/auth.js";

export function LoginPage() {
    const element = document.createElement("section");
    element.className = "auth-page";

    if (!isLoginAvailable()) {
        element.innerHTML = `<h1>Log in</h1>`;
        element.append(createNotBuiltNotice({
            feature: "Logging in",
            endpoints: ["POST /api/login", "POST /api/logout", "GET /api/me", "POST /api/register"]
        }));

        return { element, mount() { setTitle("Log in"); } };
    }

    element.innerHTML = `
        <h1>Log in</h1>
        <form class="form card auth-form">
            <label>
                Email
                <input name="username" type="email" autocomplete="username" required>
            </label>
            <label>
                Password
                <input name="password" type="password" autocomplete="current-password" required>
            </label>
        </form>
        <p class="muted">New here? <a href="/register" data-navigo>Create an account</a></p>
    `;

    // Spring's formLogin reads the fields "username" and "password", so the email
    // field is called "username"
    const form = element.querySelector("form");
    const errorMessage = createErrorMessage();
    const button = createButton({ label: "Log in", type: "submit", size: "large" });

    form.append(errorMessage.element, button.element);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        errorMessage.hide();
        button.setLoading(true);

        try {
            await login(new FormData(form));
            await refreshUser();
            router.navigate(takeReturnPath());
        } catch (error) {
            errorMessage.show(error.status === 401 ? "Wrong email or password." : error.message);
        } finally {
            button.setLoading(false);
        }
    });

    return {
        element,
        mount() {
            setTitle("Log in");
            form.elements.username.focus();
        }
    };
}
