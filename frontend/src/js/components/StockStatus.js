// components/StockStatus.js

// "In stock", "Only 3 left" or "Out of stock", coloured by stock-status.css
export function createStockStatus({ stock, lowStockLimit = 5 }) {
    const element = document.createElement("span");

    element.className = "stock-status";

    if (stock === 0) {
        element.dataset.level = "out";
        element.textContent = "Out of stock";
    } else if (stock <= lowStockLimit) {
        element.dataset.level = "low";
        element.textContent = `Only ${stock} left`;
    } else {
        element.dataset.level = "ok";
        element.textContent = "In stock";
    }

    return element;
}
