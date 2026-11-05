// pages/ProfilePage.js
//
// The logged-in customer's name and delivery address. The email is the one they
// logged in with, so it isn't part of the form.

import { getCurrentUser } from "../auth.js";
import { createCustomerForm } from "../components/CustomerForm.js";
import { createNotice } from "../components/Notice.js";
import { handleAuthError } from "../guards.js";
import { setTitle } from "../router.js";
import { getMyProfile, saveMyProfile } from "../services/customers.js";

export function ProfilePage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let profile;

        try {
            profile = await getMyProfile();
        } catch (error) {
            if (!handleAuthError(error)) {
                element.textContent = `Could not load your profile. ${error.message}`;
            }
            return;
        }

        const heading = document.createElement("h1");
        heading.textContent = "Your profile";

        const intro = document.createElement("p");
        intro.className = "muted";
        intro.textContent = profile
            ? `Logged in as ${getCurrentUser().username}. We deliver your orders to this address.`
            : `Logged in as ${getCurrentUser().username}. Add your delivery details before your first order.`;

        const notice = createNotice();

        const form = createCustomerForm({
            customer: profile,
            submitLabel: profile ? "Save changes" : "Save details",

            async onSubmit(data) {
                notice.hide();

                try {
                    profile = await saveMyProfile(data);
                } catch (error) {
                    if (handleAuthError(error)) {
                        return;
                    }
                    throw error;
                }

                notice.show("Your details have been saved.");
            }
        });

        element.replaceChildren(heading, intro, notice.element, form);
    }

    return {
        element,
        mount() {
            setTitle("Profile");
            load();
        }
    };
}
