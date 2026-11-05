// pages/ProductsPage.js

import { addToCart } from "../cart.js";
import { createCategoryFilter } from "../components/CategoryFilter.js";
import { createErrorMessage } from "../components/ErrorMessage.js";
import { createProductList } from "../components/ProductList.js";
import { createSearchInput } from "../components/SearchInput.js";
import { setTitle } from "../router.js";
import { getCategories, getProducts } from "../services/catalog.js";

export function ProductsPage() {
    const element = document.createElement("section");
    element.textContent = "Loading...";

    let searchTimer;

    // Fills `element` once the data is here. If the user has moved on to another
    // page by then, this only fills an element that is no longer shown - harmless.
    async function load() {
        let categories;
        let products;

        try {
            // Load both at the same time
            [categories, products] = await Promise.all([getCategories(), getProducts()]);
        } catch (error) {
            element.textContent = `Could not load the products. ${error.message}`;
            return;
        }

        const header = document.createElement("div");
        header.className = "page-header";
        header.innerHTML = `
            <h1>Shop</h1>
            <p class="muted">Freshly roasted beans and everything you need to brew them.</p>
        `;

        const errorMessage = createErrorMessage();

        // The filters: sent to the backend as ?categoryId=...&q=...
        let categoryId = null;
        let searchText = "";

        const filter = createCategoryFilter({
            categories,

            onSelect(id) {
                categoryId = id;
                reloadProducts();
            }
        });

        const search = createSearchInput({
            label: "Search products",
            placeholder: "Search by name or origin",

            onSearch(text) {
                searchText = text.trim();
                // Wait until the user stops typing for a moment, instead of one request per key
                clearTimeout(searchTimer);
                searchTimer = setTimeout(reloadProducts, 250);
            }
        });

        const toolbar = document.createElement("div");
        toolbar.className = "toolbar";
        toolbar.append(filter, search.element);

        let list = createList();

        function createList() {
            return createProductList({
                products,
                emptyMessage: searchText ? `No products match "${searchText}".` : "No products in this category.",
                onAddToCart: addProductToCart,

                onError(error) {
                    errorMessage.show(error.message);
                }
            });
        }

        // Answers can arrive out of order (a slow "ken" after a fast "kenya"):
        // only the answer to the newest request is shown
        let latestRequest = 0;

        async function reloadProducts() {
            const request = ++latestRequest;

            try {
                const result = await getProducts({ categoryId, search: searchText });

                if (request !== latestRequest) {
                    return;
                }

                products = result;
                errorMessage.hide();
            } catch (error) {
                errorMessage.show(`Could not load the products. ${error.message}`);
                return;
            }

            const newList = createList();
            list.replaceWith(newList);
            list = newList;
        }

        // Called by a product card. Throwing makes the card call onError above.
        async function addProductToCart(product) {
            await addToCart(product.id, 1);
            errorMessage.hide();
        }

        element.replaceChildren(header, errorMessage.element, toolbar, list);
    }

    return {
        element,
        mount() {
            setTitle("Shop");
            load();
        },
        unmount() {
            // No search request after the user has left the page
            clearTimeout(searchTimer);
        }
    };
}
