// pages/AssistantPage.js
//
// A chat with the shop's AI assistant. The backend (Spring AI) answers, and it
// remembers the conversation of the logged-in user, so this page only sends the
// new message. The chat on the screen starts empty again after a reload.

import { createButton } from "../components/Button.js";
import { createChatMessage } from "../components/ChatMessage.js";
import { createNotBuiltNotice } from "../components/NotBuiltNotice.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { sendChatMessage } from "../services/assistant.js";

const EXAMPLES = [
    "Which light roasts do you have?",
    "What do I need to brew pour-over coffee?",
    "What was in my last order?"
];

export function AssistantPage() {
    const element = document.createElement("section");
    element.className = "assistant";

    element.innerHTML = `
        <h1>Assistant</h1>
        <p class="muted">Ask about our coffee and equipment, or about your orders.</p>
        <ol class="chat-log" aria-live="polite"></ol>
        <div class="chat-examples"></div>
        <form class="chat-form">
            <label class="chat-form__label">
                <span class="visually-hidden">Message</span>
                <textarea name="message" rows="2" maxlength="2000" required
                          placeholder="Ask something... (Enter sends, Shift+Enter for a new line)"></textarea>
            </label>
        </form>
    `;

    const log = element.querySelector(".chat-log");
    const examples = element.querySelector(".chat-examples");
    const form = element.querySelector(".chat-form");
    const input = form.elements.message;
    const sendButton = createButton({ label: "Send", type: "submit" });

    form.append(sendButton.element);

    for (const example of EXAMPLES) {
        const button = createButton({
            label: example,
            variant: "secondary",
            size: "small",
            onClick() {
                send(example);
            }
        });

        examples.append(button.element);
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        send(input.value);
    });

    // Enter sends, Shift+Enter adds a new line
    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    let busy = false;

    async function send(text) {
        const message = text.trim();

        if (!message || busy) {
            return;
        }

        busy = true;
        sendButton.setLoading(true);
        examples.hidden = true;
        input.value = "";

        addMessage("user", message);
        const answer = addMessage("assistant", "Thinking...");
        answer.element.classList.add("chat-message--pending");

        try {
            const { reply } = await sendChatMessage(message);
            answer.element.classList.remove("chat-message--pending");
            answer.setText(reply);
        } catch (error) {
            answer.element.remove();

            if (handleAuthError(error)) {
                return;
            }

            if (error.status === 404) {
                element.querySelector(".chat-log").replaceWith(createNotBuiltNotice({
                    feature: "The assistant",
                    endpoints: ["POST /api/assistant/chat"]
                }));
                form.hidden = true;
                return;
            }

            addMessage("error", `No answer. ${error.message}`);
        } finally {
            busy = false;
            sendButton.setLoading(false);
            input.focus();
        }
    }

    function addMessage(role, text) {
        const message = createChatMessage({ role, text });
        log.append(message.element);
        message.element.scrollIntoView({ block: "nearest" });
        return message;
    }

    return {
        element,
        mount() {
            setTitle("Assistant");
            input.focus();
        }
    };
}
