// components/ChatMessage.js

// One message in the assistant's chat. role: "user", "assistant" or "error".
// The text goes in with textContent: an answer from a language model is untrusted
// input, just like anything a user types, so it must never become HTML.
export function createChatMessage({ role, text }) {
    const element = document.createElement("li");

    element.className = `chat-message chat-message--${role}`;

    const label = document.createElement("span");
    label.className = "chat-message__label";
    label.textContent = role === "user" ? "You" : "Assistant";

    const body = document.createElement("p");
    body.className = "chat-message__text";
    body.textContent = text;

    element.append(label, body);

    return {
        element,

        setText(value) {
            body.textContent = value;
        },

        setRole(value) {
            element.className = `chat-message chat-message--${value}`;
        }
    };
}
