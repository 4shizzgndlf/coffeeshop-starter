// components/CustomerForm.js

import { createButton } from "./Button.js";
import { createErrorMessage } from "./ErrorMessage.js";

// The fields match CustomerRequest in the backend (same names and max lengths)
const FIELDS = [
    { name: "name", label: "Name", autocomplete: "name", maxLength: 100 },
    { name: "email", label: "Email", type: "email", autocomplete: "email", maxLength: 254 },
    { name: "street", label: "Street and number", autocomplete: "street-address", maxLength: 120 },
    { name: "postalCode", label: "Postal code", autocomplete: "postal-code", maxLength: 10 },
    { name: "city", label: "City", autocomplete: "address-level2", maxLength: 60 }
];

// A form for name, email and address. customer (optional) fills in the fields.
//
// onSubmit({ name, email, street, postalCode, city }) should return a promise.
// If it rejects, the form shows why: next to the field for validation errors
// from the backend (400), above the button for anything else (e.g. 409).
export function createCustomerForm({
    customer = null,
    submitLabel = "Save",
    onSubmit
}) {
    const element = document.createElement("form");

    element.className = "form customer-form";

    const fieldErrors = {};

    for (const field of FIELDS) {
        const label = document.createElement("label");
        const input = document.createElement("input");
        const error = document.createElement("small");

        label.className = `customer-form__field customer-form__field--${field.name}`;
        label.append(field.label);

        input.name = field.name;
        input.type = field.type ?? "text";
        input.autocomplete = field.autocomplete;
        input.maxLength = field.maxLength;
        input.required = true;
        // At least one non-space character, so "   " isn't accepted
        input.pattern = ".*\\S.*";
        input.value = customer?.[field.name] ?? "";

        error.className = "customer-form__error";
        error.hidden = true;
        fieldErrors[field.name] = error;

        label.append(input, error);
        element.append(label);
    }

    const errorMessage = createErrorMessage();

    const button = createButton({
        label: submitLabel,
        type: "submit",
        size: "large"
    });

    element.append(errorMessage.element, button.element);

    function clearErrors() {
        errorMessage.hide();

        for (const error of Object.values(fieldErrors)) {
            error.textContent = "";
            error.hidden = true;
        }
    }

    function showErrors(error) {
        const fields = Object.entries(error.fieldErrors ?? {});

        for (const [name, message] of fields) {
            if (fieldErrors[name]) {
                fieldErrors[name].textContent = message;
                fieldErrors[name].hidden = false;
            }
        }

        errorMessage.show(error.message);
    }

    element.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();
        button.setLoading(true);

        const data = {};
        for (const field of FIELDS) {
            data[field.name] = element.elements[field.name].value.trim();
        }

        try {
            await onSubmit?.(data);
        } catch (error) {
            showErrors(error);
        } finally {
            button.setLoading(false);
        }
    });

    return element;
}
