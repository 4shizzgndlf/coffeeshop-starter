// services/auth.js - logging in and out, and who is logged in
//
// The backend endpoints are described in AI-PROJECT.md.

import { getJSON, sendForm, sendJSON } from "./http.js";

// The logged-in user, e.g. { username: "anna@example.com", roles: ["ROLE_CUSTOMER"] },
// or null. A 401 is a normal answer here: nobody is logged in.
export async function getMe() {
    try {
        return await getJSON("/api/me");
    } catch (error) {
        if (error.status === 401) {
            return null;
        }

        throw error;
    }
}

// formData has the fields "username" (the email) and "password".
// Spring's formLogin reads form fields, so this is not JSON. Answers 204, or 401 if wrong.
export function login(formData) {
    return sendForm("/api/login", formData);
}

export function logout() {
    return sendJSON("POST", "/api/logout");
}

// Creates a customer account. 409 if the email is already used.
export function register({ email, password }) {
    return sendJSON("POST", "/api/register", { email, password });
}
