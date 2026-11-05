// services/assistant.js - the AI assistant

import { sendJSON } from "./http.js";

// Sends one message and resolves to the assistant's answer: { reply: "..." }.
// The backend remembers the conversation of the logged-in user, so only the new
// message is sent.
export function sendChatMessage(message) {
    return sendJSON("POST", "/api/assistant/chat", { message });
}
