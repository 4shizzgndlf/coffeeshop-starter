// pages/productPage.js

import { getProduct } from "../api/catalog.js";
import { addToCart } from "../api/carts.js";
import { createButton } from "../components/Button.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createNotice } from "../components/Notice.js";
import { createProductImage } from "../components/ProductImage.js";
import { createQuantityInput } from "../components/QuantityInput.js";
import { createStockStatus } from "../components/StockStatus.js";
import { formatPrice, formatProductDetails } from "../format.js";
import { setTitle } from "../router.js";
import { ensureCartId } from "../session.js";

export async function renderProductPage(container, { id }) {
    container.textContent = "Loading...";

    let product;

    try {
        product = await getProduct(id);
    } catch (error) {
        container.innerHTML = `
            <a class="back-link" href="/" data-navigo>← All products</a>
            <p class="empty-state"></p>
        `;
        container.querySelector(".empty-state").textContent = error.status === 404
            ? "This product does not exist."
            : `Could not load the product. ${error.message}`;
        return;
    }

    setTitle(product.name);

    container.innerHTML = `
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

    container.querySelector(".product-detail__image").append(createProductImage({ product }));
    container.querySelector(".product-detail__category").textContent = product.categoryName;
    container.querySelector(".product-detail__name").textContent = product.name;
    container.querySelector(".product-detail__details").textContent = formatProductDetails(product);
    container.querySelector(".product-detail__description").textContent = product.description ?? "";
    container.querySelector(".product-detail__price").textContent = formatPrice(product.price);
    container.querySelector(".product-detail__stock").append(createStockStatus({ stock: product.stock }));

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
                const cartId = await ensureCartId();
                await addToCart(cartId, product.id, quantity.getValue());
                notice.show("Added to your cart.", { href: "/cart", label: "Go to cart →" });
            } catch (error) {
                errorMessage.show(error.message);
            } finally {
                button.setLoading(false);
            }
        }
    });

    const buy = container.querySelector(".product-detail__buy");
    buy.append(quantity.element, button.element);
    buy.after(notice.element, errorMessage.element);
}
