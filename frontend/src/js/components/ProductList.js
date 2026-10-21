// components/ProductList.js

import { createProductCard } from "./ProductCard.js";

// A grid of product cards, or a message when there are no products
export function createProductList({
    products,
    emptyMessage = "No products found.",
    onAddToCart,
    onError
}) {
    if (products.length === 0) {
        const empty = document.createElement("p");
        empty.className = "product-list__empty";
        empty.textContent = emptyMessage;
        return empty;
    }

    const element = document.createElement("ul");

    element.className = "product-list";

    for (const product of products) {
        const item = document.createElement("li");
        item.append(createProductCard({ product, onAddToCart, onError }));
        element.append(item);
    }

    return element;
}
