// pages/ordersPage.js

import { getOrders } from "../api/orders.js";
import { createOrderRow } from "../components/OrderRow.js";
import { forgetCustomer, getCustomerId } from "../session.js";

export async function renderOrdersPage(container) {
    const customerId = getCustomerId();

    if (customerId === null) {
        renderNoOrders(container);
        return;
    }

    container.textContent = "Loading...";

    let orders;

    try {
        orders = await getOrders(customerId);
    } catch (error) {
        // The stored customer no longer exists, e.g. after the database was reset
        if (error.status === 404) {
            forgetCustomer();
            renderNoOrders(container);
            return;
        }

        container.textContent = `Could not load your orders. ${error.message}`;
        return;
    }

    if (orders.length === 0) {
        renderNoOrders(container);
        return;
    }

    const heading = document.createElement("h1");
    heading.textContent = "My orders";

    const list = document.createElement("ul");
    list.className = "order-list";

    for (const order of orders) {
        list.append(createOrderRow({ order }));
    }

    container.replaceChildren(heading, list);
}

function renderNoOrders(container) {
    container.innerHTML = `
        <h1>My orders</h1>
        <p class="empty-state">
            No orders yet. Orders you place in this browser show up here.
            Ordered on another device? <a href="/profile" data-navigo>Find your profile</a>.
        </p>
    `;
}
