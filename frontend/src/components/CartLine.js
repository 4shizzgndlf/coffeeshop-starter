// components/CartLine.js

import { formatPrice } from "../format.js";
import { createButton } from "./Button.js";
import { createQuantityInput } from "./QuantityInput.js";

// One product in the cart. onQuantityChange(line, quantity) and onRemove(line)
// are handled by the page, which saves the change and redraws the cart.
export function createCartLine({
    line,
    onQuantityChange,
    onRemove
}) {
    const element = document.createElement("li");

    element.className = "cart-line";

    element.innerHTML = `
        <div class="cart-line__product">
            <a class="cart-line__name" data-navigo></a>
            <span class="cart-line__price"></span>
            <span class="cart-line__warning" role="alert" hidden></span>
        </div>
        <span class="cart-line__total"></span>
      `;

    const name = element.querySelector(".cart-line__name");
    name.setAttribute("href", `/products/${line.productId}`);
    name.textContent = line.productName;

    element.querySelector(".cart-line__price").textContent = `${formatPrice(line.unitPrice)} each`;
    element.querySelector(".cart-line__total").textContent = formatPrice(line.lineTotal);

    // The stock may have dropped since the product was added
    if (line.quantity > line.available) {
        const warning = element.querySelector(".cart-line__warning");
        warning.textContent = line.available === 0
            ? "Out of stock - please remove it"
            : `Only ${line.available} left - please lower the quantity`;
        warning.hidden = false;
    }

    const quantity = createQuantityInput({
        value: line.quantity,
        // At least 1, so a product that sold out can still be shown (and removed)
        max: Math.max(1, Math.min(line.available, 99)),
        label: `Quantity of ${line.productName}`,

        onChange(value) {
            quantity.setDisabled(true);
            onQuantityChange?.(line, value);
        }
    });

    const removeButton = createButton({
        label: "Remove",
        variant: "secondary",
        size: "small",

        onClick() {
            removeButton.setLoading(true);
            onRemove?.(line);
        }
    });

    element.querySelector(".cart-line__product").after(quantity.element);
    element.append(removeButton.element);

    return element;
}
