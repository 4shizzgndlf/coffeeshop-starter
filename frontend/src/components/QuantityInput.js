// components/QuantityInput.js

// A number field with − and + buttons, kept between min and max.
// onChange(quantity) is called when the user changes the value.
export function createQuantityInput({
    value = 1,
    min = 1,
    max = 99,
    label = "Quantity",
    onChange
} = {}) {
    const element = document.createElement("div");

    element.className = "quantity-input";

    element.innerHTML = `
        <button class="quantity-input__step" type="button" aria-label="Decrease">−</button>
        <input class="quantity-input__field" type="number" inputmode="numeric">
        <button class="quantity-input__step" type="button" aria-label="Increase">+</button>
      `;

    const [decrease, increase] = element.querySelectorAll(".quantity-input__step");
    const input = element.querySelector(".quantity-input__field");

    input.min = String(min);
    input.max = String(max);
    input.setAttribute("aria-label", label);

    let current = value;
    let disabled = false;

    function render() {
        input.value = String(current);
        input.disabled = disabled;
        decrease.disabled = disabled || current <= min;
        increase.disabled = disabled || current >= max;
    }

    function update(newValue) {
        const clamped = Math.min(max, Math.max(min, Math.round(newValue) || min));
        const changed = clamped !== current;

        current = clamped;
        render();

        if (changed) {
            onChange?.(current);
        }
    }

    decrease.addEventListener("click", () => update(current - 1));
    increase.addEventListener("click", () => update(current + 1));
    // "change" fires when the user leaves the field or presses Enter, not on every key
    input.addEventListener("change", () => update(Number(input.value)));

    render();

    return {
        element,

        getValue() {
            return current;
        },

        setDisabled(value) {
            disabled = value;
            render();
        }
    };
}
