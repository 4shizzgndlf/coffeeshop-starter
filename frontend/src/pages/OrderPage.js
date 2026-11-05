// pages/OrderPage.js

import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createOrderLineTable } from "../components/OrderLineTable.js";
import { createStatusBadge } from "../components/StatusBadge.js";
import { formatDate } from "../format.js";
import { setTitle } from "../router.js";
import { cancelOrder, getOrder } from "../services/orders.js";

export function OrderPage({ id }) {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    const errorMessage = createErrorMessage();

    let order;

    async function load() {
        try {
            order = await getOrder(id);
        } catch (error) {
            element.innerHTML = `
                <a class="back-link" href="/orders" data-navigo>← My orders</a>
                <p class="empty-state"></p>
            `;
            element.querySelector(".empty-state").textContent = error.status === 404
                ? "This order does not exist."
                : `Could not load the order. ${error.message}`;
            return;
        }

        // Only while this page is still shown: the user may have moved on during the load
        if (element.isConnected) {
            setTitle(`Order #${order.id}`);
        }

        renderOrder();
    }

    // Draws the whole page from `order` - called again after cancelling
    function renderOrder() {
        element.innerHTML = `
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

        element.querySelector("h1").textContent = `Order #${order.id}`;
        element.querySelector(".order-detail__header").append(createStatusBadge({ status: order.status }));
        element.querySelector(".order-detail__date").textContent = `Placed ${formatDate(order.placedAt)}`;
        element.querySelector(".order-detail__lines").append(createOrderLineTable({ order }));

        // One line per part of the address, each set with textContent
        const address = element.querySelector(".order-detail__address");
        const { recipientName, street, postalCode, city } = order.shippingAddress;

        for (const text of [recipientName, street, `${postalCode} ${city}`]) {
            const line = document.createElement("span");
            line.textContent = text;
            address.append(line);
        }

        element.querySelector(".back-link").after(errorMessage.element);

        // Only a placed order can be cancelled (the backend checks it too)
        if (order.status === "PLACED") {
            const cancelButton = createButton({
                label: "Cancel order",
                variant: "danger",
                onClick: cancel
            });

            element.querySelector(".order-detail__actions").append(cancelButton.element);
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

    return {
        element,
        mount() {
            // Replaced by "Order #3" once the order has loaded
            setTitle("Order");
            load();
        }
    };
}
