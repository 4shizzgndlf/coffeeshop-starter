// pages/notFoundPage.js

export function renderNotFoundPage(container) {
    container.innerHTML = `
        <h1>Page not found</h1>
        <p>There is nothing at this address.</p>
        <a href="/" data-navigo>Go to the shop</a>
    `;
}
