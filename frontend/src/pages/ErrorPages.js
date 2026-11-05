// pages/ErrorPages.js

import { setTitle } from "../router.js";

// textContent, so a message from the server can't add HTML
function errorPage(title, text) {
    const section = document.createElement("section");
    section.classList.add("error-page");

    section.innerHTML = `
        <h1></h1>
        <p></p>
    `;

    section.querySelector("h1").textContent = title;
    section.querySelector("p").textContent = text;

    return section;
}

export function NotFoundPage() {
    const element = errorPage("Page not found", "There is nothing at this address.");
    element.insertAdjacentHTML("beforeend", `<a href="/" data-navigo>Go to the shop</a>`);

    return {
        element,
        mount() {
            setTitle("Not found");
        }
    };
}

// Shown by the guards (guards.js) when the user is logged in, but without the role
// the page needs, e.g. staff on "My orders"
export function ForbiddenPage() {
    const element = errorPage("Not for you", "Your account can't use this page.");
    element.insertAdjacentHTML("beforeend", `<a href="/" data-navigo>Go to the shop</a>`);

    return {
        element,
        mount() {
            setTitle("Forbidden");
        }
    };
}

// Shown by app.js instead of the routes when the API can't be reached at startup.
// message is the ApiError's, e.g. "Cannot reach the API (HTTP 502). Is Spring Boot running?"
// No link to the shop: the routes aren't set up, so the user has to reload.
export function ServerDownPage({ message }) {
    return {
        element: errorPage("Can't reach the server", message),
        mount() {
            setTitle("Server down");
        }
    };
}
