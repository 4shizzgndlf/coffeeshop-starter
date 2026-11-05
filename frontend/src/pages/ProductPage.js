// pages/ProductPage.js

import { addToCart } from "../cart.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createNotice } from "../components/Notice.js";
import { createProductImage } from "../components/ProductImage.js";
import { createQuantityInput } from "../components/QuantityInput.js";
import { createStockStatus } from "../components/StockStatus.js";
import { formatPrice, formatProductDetails } from "../format.js";
import { setTitle } from "../router.js";
import { getProduct } from "../services/catalog.js";

export function ProductPage({ id }) {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    async function load() {
        let product;

        try {
            product = await getProduct(id);
        } catch (error) {
            element.innerHTML = `
                <a class="back-link" href="/" data-navigo>← All products</a>
                <p class="empty-state"></p>
            `;
            element.querySelector(".empty-state").textContent = error.status === 404
                ? "This product does not exist."
                : `Could not load the product. ${error.message}`;
            return;
        }

        // Only while this page is still shown: the user may have moved on during the load
        if (element.isConnected) {
            setTitle(product.name);
        }

        element.innerHTML = `
            <a class="back-link" href="/" data-navigo>← All products</a>
            <article class="product-detail">
                <div class="product-detail__image"></div>
                <div class="product-detail__info">
                    <p class="product-detail__category"></p>
                    <h1 class="product-detail__name"></h1>
                    <p class="product-detail__details"></p>
                    <p class="product-detail__description"></p>
                    <p class="product-detail__price"></p>
                    <div class="product-detail__stock"></div>
                    <div class="product-detail__buy"></div>
                </div>
            </article>
        `;

        element.querySelector(".product-detail__image").append(createProductImage({ product }));
        element.querySelector(".product-detail__category").textContent = product.categoryName;
        element.querySelector(".product-detail__name").textContent = product.name;
        element.querySelector(".product-detail__details").textContent = formatProductDetails(product);
        element.querySelector(".product-detail__description").textContent = product.description ?? "";
        element.querySelector(".product-detail__price").textContent = formatPrice(product.price);
        element.querySelector(".product-detail__stock").append(createStockStatus({ stock: product.stock }));

        if (product.stock === 0) {
            return;
        }

        const notice = createNotice();
        const errorMessage = createErrorMessage();

        const quantity = createQuantityInput({
            max: Math.min(product.stock, 99),
            label: `Quantity of ${product.name}`
        });

        const button = createButton({
            label: "Add to cart",
            size: "large",

            async onClick() {
                button.setLoading(true);
                notice.hide();
                errorMessage.hide();

                try {
                    await addToCart(product.id, quantity.getValue());
                    notice.show("Added to your cart.", { href: "/cart", label: "Go to cart →" });
                } catch (error) {
                    errorMessage.show(error.message);
                } finally {
                    button.setLoading(false);
                }
            }
        });

        const buy = element.querySelector(".product-detail__buy");
        buy.append(quantity.element, button.element);
        buy.after(notice.element, errorMessage.element);
    }

    return {
        element,
        mount() {
            // Replaced by the product's name once it has loaded
            setTitle("Product");
            load();
        }
    };
}
