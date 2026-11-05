// components/StatusBadge.js

import { formatEnum } from "../format.js";

// An order status (PLACED, SHIPPED, CANCELLED) as a coloured label
export function createStatusBadge({ status }) {
    const element = document.createElement("span");

    element.className = "status-badge";
    element.dataset.status = status;
    element.textContent = formatEnum(status);

    return element;
}
