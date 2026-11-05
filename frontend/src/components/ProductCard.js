// components/ProductCard.js

import { formatPrice, formatProductDetails } from "../format.js";
import { createButton } from "./Button.js";
import { createProductImage } from "./ProductImage.js";
import { createStockStatus } from "./StockStatus.js";

// onAddToCart(product) should return a promise. While it runs the button shows
// "Loading...", then "Added ✓" for a moment. If it rejects, onError(error) is called.
export function createProductCard({
    product,
    onAddToCart,
    onError
}) {
    const element = document.createElement("article");

    element.className = "product-card";

    element.innerHTML = `
        <a class="product-card__image" data-navigo tabindex="-1" aria-hidden="true"></a>
        <div class="product-card__body">
            <p class="product-card__category"></p>
            <h2 class="product-card__name"><a data-navigo></a></h2>
            <p class="product-card__details"></p>
        </div>
        <div class="product-card__footer">
            <span class="product-card__price"></span>
        </div>
      `;

    const href = `/products/${product.id}`;

    const imageLink = element.querySelector(".product-card__image");
    imageLink.setAttribute("href", href);
    imageLink.append(createProductImage({ product }));

    const nameLink = element.querySelector(".product-card__name a");
    nameLink.setAttribute("href", href);
    nameLink.textContent = product.name;

    element.querySelector(".product-card__category").textContent = product.categoryName;
    element.querySelector(".product-card__details").textContent = formatProductDetails(product);
    element.querySelector(".product-card__body").append(createStockStatus({ stock: product.stock }));
    element.querySelector(".product-card__price").textContent = formatPrice(product.price);

    const soldOut = product.stock === 0;

    const button = createButton({
        label: soldOut ? "Sold out" : "Add to cart",
        size: "small",
        disabled: soldOut,

        async onClick() {
            button.setLoading(true);

            try {
                await onAddToCart?.(product);
                button.setLoading(false);
                button.setLabel("Added ✓");
                setTimeout(() => button.setLabel("Add to cart"), 1500);
            } catch (error) {
                button.setLoading(false);
                onError?.(error);
            }
        }
    });

    element.querySelector(".product-card__footer").append(button.element);

    return element;
}
