// pages/StaffOrdersPage.js
//
// Staff only (requireRole in app.js, and Spring Security in the backend): every
// customer's orders. Open one to mark it as shipped.

import { createNotBuiltNotice } from "../components/NotBuiltNotice.js";
import { createOrderRow } from "../components/OrderRow.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { getAllOrders } from "../services/orders.js";

export function StaffOrdersPage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let orders;

        try {
            orders = await getAllOrders();
        } catch (error) {
            if (handleAuthError(error)) {
                return;
            }

            if (error.status === 404) {
                element.innerHTML = `<h1>All orders</h1>`;
                element.append(createNotBuiltNotice({
                    feature: "The staff order list",
                    endpoints: ["GET /api/staff/orders"]
                }));
                return;
            }

            element.textContent = `Could not load the orders. ${error.message}`;
            return;
        }

        const heading = document.createElement("h1");
        heading.textContent = "All orders";

        const intro = document.createElement("p");
        intro.className = "muted";
        const toShip = orders.filter(order => order.status === "PLACED").length;
        intro.textContent = `${orders.length} orders, ${toShip} waiting to be shipped. Open an order to ship it.`;

        const list = document.createElement("ul");
        list.className = "order-list";

        for (const order of orders) {
            list.append(createOrderRow({ order }));
        }

        element.replaceChildren(heading, intro, list);
    }

    return {
        element,
        mount() {
            setTitle("All orders");
            load();
        }
    };
}
