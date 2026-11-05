// components/NotBuiltNotice.js

// Shown when the backend answers 404 for an endpoint the frontend needs: that part of
// the backend isn't built yet. endpoints: ["POST /api/login", "GET /api/me"]
export function createNotBuiltNotice({ feature, endpoints }) {
    const element = document.createElement("div");

    element.className = "card not-built";
    element.innerHTML = `
        <h2></h2>
        <p>The frontend is ready, but the backend doesn't have these endpoints yet:</p>
        <ul class="not-built__endpoints"></ul>
        <p class="muted">See <strong>AI-PROJECT.md</strong> for what they should do.</p>
    `;

    element.querySelector("h2").textContent = `${feature} isn't built yet`;

    const list = element.querySelector(".not-built__endpoints");

    for (const endpoint of endpoints) {
        const item = document.createElement("li");
        const code = document.createElement("code");
        code.textContent = endpoint;
        item.append(code);
        list.append(item);
    }

    return element;
}
