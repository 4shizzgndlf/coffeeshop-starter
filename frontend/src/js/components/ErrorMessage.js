// components/ErrorMessage.js

// A message box for errors, hidden until show() is called.
// role="alert" makes screen readers announce it when it appears.
export function createErrorMessage() {
    const element = document.createElement("p");

    element.className = "error-message";
    element.setAttribute("role", "alert");
    element.hidden = true;

    return {
        element,

        show(message) {
            element.textContent = message;
            element.hidden = false;
        },

        hide() {
            element.textContent = "";
            element.hidden = true;
        }
    };
}
