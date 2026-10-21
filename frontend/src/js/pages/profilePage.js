// pages/profilePage.js
//
// Without a login, "who am I" is just a customer id in localStorage (see session.js).
// This page creates that profile, finds an existing one by email, or edits it.
// When Spring Security is added, this becomes the "my account" page.

import { createCustomer, findCustomerByEmail, updateCustomer } from "../api/customers.js";
import { createButton } from "../components/Button.js";
import { createCustomerForm } from "../components/CustomerForm.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createNotice } from "../components/Notice.js";
import { forgetCustomer, loadCustomer, setCustomerId } from "../session.js";

export async function renderProfilePage(container) {
    container.textContent = "Loading...";

    let customer;

    try {
        customer = await loadCustomer();
    } catch (error) {
        container.textContent = `Could not load your profile. ${error.message}`;
        return;
    }

    if (customer) {
        renderExistingCustomer();
    } else {
        renderNewCustomer();
    }

    // ---------- A known customer: edit the details ----------

    function renderExistingCustomer(message) {
        const heading = document.createElement("h1");
        heading.textContent = "Your profile";

        const intro = document.createElement("p");
        intro.className = "muted";
        intro.textContent = "We use these details to deliver your orders.";

        const notice = createNotice();

        if (message) {
            notice.show(message);
        }

        const form = createCustomerForm({
            customer,
            submitLabel: "Save changes",

            async onSubmit(data) {
                notice.hide();
                customer = await updateCustomer(customer.id, data);
                notice.show("Your profile has been saved.");
            }
        });

        const forgetButton = createButton({
            label: "Not you? Forget this profile",
            variant: "secondary",
            size: "small",

            onClick() {
                forgetCustomer();
                customer = null;
                renderNewCustomer();
            }
        });

        const footer = document.createElement("p");
        footer.className = "profile-footer";
        footer.append(forgetButton.element);

        container.replaceChildren(heading, intro, notice.element, form, footer);
    }

    // ---------- No customer yet: find one by email, or create one ----------

    function renderNewCustomer() {
        const heading = document.createElement("h1");
        heading.textContent = "Profile";

        const lookup = document.createElement("section");
        lookup.className = "card";
        lookup.innerHTML = `
            <h2>Ordered before?</h2>
            <p class="muted">Enter your email to use your saved details and see your orders in this browser.</p>
            <form class="form email-lookup">
                <label>
                    Email
                    <input name="email" type="email" autocomplete="email" required>
                </label>
            </form>
        `;

        const lookupForm = lookup.querySelector("form");
        const lookupError = createErrorMessage();
        const lookupButton = createButton({ label: "Find my profile", type: "submit" });

        lookupForm.append(lookupError.element, lookupButton.element);

        lookupForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            lookupError.hide();
            lookupButton.setLoading(true);

            try {
                const found = await findCustomerByEmail(lookupForm.elements.email.value.trim());

                if (!found) {
                    lookupError.show("No customer has that email. Create a profile below instead.");
                    return;
                }

                customer = found;
                setCustomerId(customer.id);
                renderExistingCustomer(`Welcome back, ${customer.name}!`);
            } catch (error) {
                lookupError.show(error.message);
            } finally {
                lookupButton.setLoading(false);
            }
        });

        const create = document.createElement("section");
        create.innerHTML = `<h2>New customer</h2>`;
        create.append(createCustomerForm({
            submitLabel: "Create profile",

            async onSubmit(data) {
                customer = await createCustomer(data);
                setCustomerId(customer.id);
                renderExistingCustomer("Your profile has been created.");
            }
        }));

        container.replaceChildren(heading, lookup, create);
    }
}
