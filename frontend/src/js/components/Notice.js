// components/Notice.js

// A message box for good news ("Added to your cart"), hidden until show() is called.
// The counterpart of ErrorMessage. role="status" lets screen readers announce it politely.
export function createNotice() {
    const element = document.createElement("p");

    element.className = "notice";
    element.setAttribute("role", "status");
    element.hidden = true;

    return {
        element,

        // link is optional: { href: "/cart", label: "Go to cart" }
        show(message, link) {
            element.textContent = message;

            if (link) {
                const a = document.createElement("a");
                a.setAttribute("href", link.href);
                a.setAttribute("data-navigo", "");
                a.textContent = link.label;
                element.append(" ", a);
            }

            element.hidden = false;
        },

        hide() {
            element.textContent = "";
            element.hidden = true;
        }
    };
}
