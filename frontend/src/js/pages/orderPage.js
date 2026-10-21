// pages/orderPage.js

import { cancelOrder, getOrder } from "../api/orders.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createOrderLineTable } from "../components/OrderLineTable.js";
import { createStatusBadge } from "../components/StatusBadge.js";
import { formatDate } from "../format.js";
import { setTitle } from "../router.js";

export async function renderOrderPage(container, { id }) {
    container.textContent = "Loading...";

    let order;

    try {
        order = await getOrder(id);
    } catch (error) {
        container.innerHTML = `
            <a class="back-link" href="/orders" data-navigo>← My orders</a>
            <p class="empty-state"></p>
        `;
        container.querySelector(".empty-state").textContent = error.status === 404
            ? "This order does not exist."
            : `Could not load the order. ${error.message}`;
        return;
    }

    setTitle(`Order #${order.id}`);

    const errorMessage = createErrorMessage();

    // Draws the whole page from `order` - called again after cancelling
    function renderOrder() {
        container.innerHTML = `
            <a class="back-link" href="/orders" data-navigo>← My orders</a>
            <div class="order-detail__header">
                <h1></h1>
            </div>
            <p class="muted order-detail__date"></p>
            <div class="layout-with-sidebar">
                <section class="order-detail__lines">
                    <h2>Items</h2>
                </section>
                <aside class="card">
                    <h2>Shipping to</h2>
                    <address class="order-detail__address"></address>
                    <div class="order-detail__actions"></div>
                </aside>
            </div>
        `;

        container.querySelector("h1").textContent = `Order #${order.id}`;
        container.querySelector(".order-detail__header").append(createStatusBadge({ status: order.status }));
        container.querySelector(".order-detail__date").textContent = `Placed ${formatDate(order.placedAt)}`;
        container.querySelector(".order-detail__lines").append(createOrderLineTable({ order }));

        // One line per part of the address, each set with textContent
        const address = container.querySelector(".order-detail__address");
        const { recipientName, street, postalCode, city } = order.shippingAddress;

        for (const text of [recipientName, street, `${postalCode} ${city}`]) {
            const line = document.createElement("span");
            line.textContent = text;
            address.append(line);
        }

        container.querySelector(".back-link").after(errorMessage.element);

        // Only a placed order can be cancelled (the backend checks it too)
        if (order.status === "PLACED") {
            const cancelButton = createButton({
                label: "Cancel order",
                variant: "danger",
                onClick: cancel
            });

            container.querySelector(".order-detail__actions").append(cancelButton.element);
        }
    }

    async function cancel() {
        if (!confirm(`Cancel order #${order.id}?`)) {
            return;
        }

        try {
            order = await cancelOrder(order.id);
            errorMessage.hide();
        } catch (error) {
            errorMessage.show(error.message);
        }

        renderOrder();
    }

    renderOrder();
}
