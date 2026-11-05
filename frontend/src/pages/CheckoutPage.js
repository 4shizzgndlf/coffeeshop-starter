// pages/CheckoutPage.js

import { refreshCart } from "../cart.js";
import { createCartSummary } from "../components/CartSummary.js";
import { createCustomerForm } from "../components/CustomerForm.js";
import { formatPrice } from "../format.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { getMyProfile, saveMyProfile } from "../services/customers.js";
import { placeOrder } from "../services/orders.js";

export function CheckoutPage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let cart;
        let profile;

        try {
            [cart, profile] = await Promise.all([refreshCart(), getMyProfile()]);
        } catch (error) {
            if (!handleAuthError(error)) {
                element.textContent = `Could not load the checkout. ${error.message}`;
            }
            return;
        }

        if (!cart || cart.lines.length === 0) {
            element.innerHTML = `
                <h1>Checkout</h1>
                <p class="empty-state">Your cart is empty. <a href="/" data-navigo>Browse the shop</a></p>
            `;
            return;
        }

        const heading = document.createElement("h1");
        heading.textContent = "Checkout";

        const details = document.createElement("section");
        details.innerHTML = `<h2>Delivery details</h2>`;

        const form = createCustomerForm({
            customer: profile,
            submitLabel: `Place order · ${formatPrice(cart.total)}`,

            // Errors thrown here (e.g. 409 "Only 2 left of ...") are shown by the form
            async onSubmit(data) {
                let order;

                try {
                    // Saves the delivery details on the profile first: the order copies them
                    await saveMyProfile(data);
                    order = await placeOrder({ cartId: cart.id });
                } catch (error) {
                    if (handleAuthError(error)) {
                        return;
                    }
                    throw error;
                }

                // The backend emptied the cart. Loading it again tells the badge in the header
                refreshCart().catch(console.error);

                showConfirmation(order);
            }
        });

        details.append(form);

        const layout = document.createElement("div");
        layout.className = "layout-with-sidebar";
        layout.append(details, createCartSummary({ cart, showLines: true }));

        element.replaceChildren(heading, layout);
    }

    function showConfirmation(order) {
        setTitle("Thank you");
        window.scrollTo(0, 0);

        element.innerHTML = `
            <div class="confirmation">
                <h1>Thank you for your order!</h1>
                <p class="confirmation__text"></p>
                <p class="confirmation__links">
                    <a class="order-link" data-navigo>View your order</a>
                    <a href="/" data-navigo>Continue shopping</a>
                </p>
            </div>
        `;

        const address = order.shippingAddress;
        element.querySelector(".confirmation__text").textContent =
            `Order #${order.id} has been placed and will be sent to ${address.recipientName}, ${address.city}.`;
        element.querySelector(".order-link").setAttribute("href", `/orders/${order.id}`);
    }

    return {
        element,
        mount() {
            setTitle("Checkout");
            load();
        }
    };
}
