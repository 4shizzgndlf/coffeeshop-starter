// pages/RegisterPage.js

import { isLoginAvailable, refreshUser } from "../auth.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createNotBuiltNotice } from "../components/NotBuiltNotice.js";
import { takeReturnPath } from "../guards.js";
import { router, setTitle } from "../router.js";
import { login, register } from "../services/auth.js";

const MIN_PASSWORD_LENGTH = 8;

export function RegisterPage() {
    const element = document.createElement("section");
    element.className = "auth-page";

    if (!isLoginAvailable()) {
        element.innerHTML = `<h1>Create an account</h1>`;
        element.append(createNotBuiltNotice({
            feature: "Creating an account",
            endpoints: ["POST /api/register", "POST /api/login", "GET /api/me"]
        }));

        return { element, mount() { setTitle("Create an account"); } };
    }

    element.innerHTML = `
        <h1>Create an account</h1>
        <form class="form card auth-form">
            <label>
                Email
                <input name="email" type="email" autocomplete="email" maxlength="254" required>
                <small class="auth-form__error" data-field="email" hidden></small>
            </label>
            <label>
                Password
                <input name="password" type="password" autocomplete="new-password" required>
                <small class="muted auth-form__hint"></small>
                <small class="auth-form__error" data-field="password" hidden></small>
            </label>
        </form>
        <p class="muted">Already have an account? <a href="/login" data-navigo>Log in</a></p>
    `;

    const form = element.querySelector("form");
    const errorMessage = createErrorMessage();
    const button = createButton({ label: "Create account", type: "submit", size: "large" });

    form.elements.password.minLength = MIN_PASSWORD_LENGTH;
    element.querySelector(".auth-form__hint").textContent = `At least ${MIN_PASSWORD_LENGTH} characters.`;
    form.append(errorMessage.element, button.element);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();
        button.setLoading(true);

        const email = form.elements.email.value.trim();
        const password = form.elements.password.value;

        try {
            await register({ email, password });

            // Log straight in with the same details, like most shops do
            const credentials = new FormData();
            credentials.set("username", email);
            credentials.set("password", password);
            await login(credentials);
            await refreshUser();

            router.navigate(takeReturnPath("/profile"));
        } catch (error) {
            showErrors(error);
        } finally {
            button.setLoading(false);
        }
    });

    // Validation errors from the backend (400) go next to their field
    function showErrors(error) {
        const fieldErrors = error.fieldErrors ?? {};

        for (const small of form.querySelectorAll(".auth-form__error")) {
            const message = fieldErrors[small.dataset.field];

            if (message) {
                small.textContent = message;
                small.hidden = false;
            }
        }

        errorMessage.show(error.message);
    }

    function clearErrors() {
        errorMessage.hide();

        for (const small of form.querySelectorAll(".auth-form__error")) {
            small.textContent = "";
            small.hidden = true;
        }
    }

    return {
        element,
        mount() {
            setTitle("Create an account");
            form.elements.email.focus();
        }
    };
}
