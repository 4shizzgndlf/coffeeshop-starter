// pages/OrderPage.js

import { hasRole } from "../auth.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createOrderLineTable } from "../components/OrderLineTable.js";
import { createStatusBadge } from "../components/StatusBadge.js";
import { formatDate } from "../format.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { cancelOrder, getOrder, shipOrder } from "../services/orders.js";

// Customers see their own order and can cancel it. Staff see any order and can ship it.
export function OrderPage({ id }) {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    const errorMessage = createErrorMessage();
    const isStaff = hasRole("ROLE_STAFF");
    const backLink = isStaff
        ? `<a class="back-link" href="/staff" data-navigo>← All orders</a>`
        : `<a class="back-link" href="/orders" data-navigo>← My orders</a>`;

    let order;

    async function load() {
        try {
            order = await getOrder(id);
        } catch (error) {
            if (handleAuthError(error)) {
                return;
            }

            // The backend also answers 404 for another customer's order
            element.innerHTML = `
                ${backLink}
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
            ${backLink}
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

        // Only a placed order can be cancelled or shipped (the backend checks it too)
        if (order.status === "PLACED") {
            const button = isStaff
                ? createButton({ label: "Mark as shipped", onClick: ship })
                : createButton({ label: "Cancel order", variant: "danger", onClick: cancel });

            element.querySelector(".order-detail__actions").append(button.element);
        }
    }

    async function cancel() {
        if (confirm(`Cancel order #${order.id}?`)) {
            await change(() => cancelOrder(order.id));
        }
    }

    async function ship() {
        await change(() => shipOrder(order.id));
    }

    // Runs the request, then draws the page again with the changed order
    async function change(request) {
        try {
            order = await request();
            errorMessage.hide();
        } catch (error) {
            if (handleAuthError(error)) {
                return;
            }
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
