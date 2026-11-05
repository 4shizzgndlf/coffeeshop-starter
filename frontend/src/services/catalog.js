// services/catalog.js - products and categories (read-only)

import { getJSON } from "./http.js";

export function getCategories() {
    return getJSON("/api/categories");
}

// Both filters are optional, e.g. getProducts({ categoryId: 1, search: "kenya" })
export function getProducts({ categoryId, search } = {}) {
    const params = new URLSearchParams();

    if (categoryId) {
        params.set("categoryId", categoryId);
    }

    if (search) {
        params.set("q", search);
    }

    const query = params.toString();

    return getJSON(query ? `/api/products?${query}` : "/api/products");
}

export function getProduct(id) {
    return getJSON(`/api/products/${id}`);
}
