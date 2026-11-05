// pages/OrdersPage.js

import { createOrderRow } from "../components/OrderRow.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { getMyOrders } from "../services/orders.js";

export function OrdersPage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let orders;

        try {
            orders = await getMyOrders();
        } catch (error) {
            if (!handleAuthError(error)) {
                element.textContent = `Could not load your orders. ${error.message}`;
            }
            return;
        }

        if (orders.length === 0) {
            renderNoOrders();
            return;
        }

        const heading = document.createElement("h1");
        heading.textContent = "My orders";

        const list = document.createElement("ul");
        list.className = "order-list";

        for (const order of orders) {
            list.append(createOrderRow({ order }));
        }

        element.replaceChildren(heading, list);
    }

    function renderNoOrders() {
        element.innerHTML = `
            <h1>My orders</h1>
            <p class="empty-state">
                No orders yet. <a href="/" data-navigo>Browse the shop</a>
            </p>
        `;
    }

    return {
        element,
        mount() {
            setTitle("My orders");
            load();
        }
    };
}
