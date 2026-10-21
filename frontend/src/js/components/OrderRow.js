// components/OrderRow.js

import { formatDate, formatPrice } from "../format.js";
import { createStatusBadge } from "./StatusBadge.js";

// One order in "My orders": number, date, item count, status and total
export function createOrderRow({ order }) {
    const element = document.createElement("li");

    element.className = "order-row";

    element.innerHTML = `
        <div class="order-row__main">
            <a class="order-row__link" data-navigo></a>
            <span class="order-row__meta"></span>
        </div>
        <span class="order-row__total"></span>
      `;

    const link = element.querySelector(".order-row__link");
    link.setAttribute("href", `/orders/${order.id}`);
    link.textContent = `Order #${order.id}`;

    const items = order.itemCount === 1 ? "1 item" : `${order.itemCount} items`;
    element.querySelector(".order-row__meta").textContent = `${formatDate(order.placedAt)} · ${items}`;
    element.querySelector(".order-row__total").textContent = formatPrice(order.total);

    element.querySelector(".order-row__main").after(createStatusBadge({ status: order.status }));

    return element;
}
