// services/customers.js - the logged-in customer's profile (name and delivery address)

import { getJSON, sendJSON } from "./http.js";

const BASE_URL = "/api/customers/me";

// The profile, or null if the customer hasn't entered their details yet (404)
export async function getMyProfile() {
    try {
        return await getJSON(BASE_URL);
    } catch (error) {
        if (error.status === 404) {
            return null;
        }

        throw error;
    }
}

// profile: { name, street, postalCode, city }. Creates the profile the first time,
// and updates it after that. The email is the one the customer logged in with.
export function saveMyProfile(profile) {
    return sendJSON("PUT", BASE_URL, profile);
}
