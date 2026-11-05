// pages/CheckoutPage.js

import { refreshCart } from "../cart.js";
import { createCartSummary } from "../components/CartSummary.js";
import { createCustomerForm } from "../components/CustomerForm.js";
import { formatPrice } from "../format.js";
import { setTitle } from "../router.js";
import { createCustomer, updateCustomer } from "../services/customers.js";
import { placeOrder } from "../services/orders.js";
import { loadCustomer, setCustomerId } from "../session.js";

export function CheckoutPage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let cart;
        let customer;

        try {
            [cart, customer] = await Promise.all([refreshCart(), loadCustomer()]);
        } catch (error) {
            element.textContent = `Could not load the checkout. ${error.message}`;
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
            customer,
            submitLabel: `Place order · ${formatPrice(cart.total)}`,

            // Errors thrown here (e.g. 409 "Only 2 left of ...") are shown by the form
            async onSubmit(data) {
                const customerId = await saveCustomer(data);
                const order = await placeOrder({ customerId, cartId: cart.id });

                // The backend emptied the cart. Loading it again tells the badge in the header
                refreshCart().catch(console.error);

                showConfirmation(order);
            }
        });

        details.append(form);

        if (!customer) {
            const hint = document.createElement("p");
            hint.className = "muted";
            hint.innerHTML = `Ordered before? <a href="/profile" data-navigo>Find your profile</a> to use your saved details.`;
            details.append(hint);
        }

        const layout = document.createElement("div");
        layout.className = "layout-with-sidebar";
        layout.append(details, createCartSummary({ cart, showLines: true }));

        element.replaceChildren(heading, layout);

        // A new customer is created the first time; after that the profile is updated.
        // Remembering `customer` matters if placing the order fails and the user tries
        // again: the second attempt must not create the same customer twice.
        async function saveCustomer(data) {
            customer = customer
                ? await updateCustomer(customer.id, data)
                : await createCustomer(data);

            setCustomerId(customer.id);

            return customer.id;
        }
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
