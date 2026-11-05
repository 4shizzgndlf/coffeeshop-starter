// components/ProductImage.js

// A drawn placeholder instead of a photo: a coffee bean coloured by roast level,
// or a cup for equipment. (Real product photos would be a nice feature to add.)
// The SVG markup is fixed text, so innerHTML is safe here.
const BEAN = `
    <svg viewBox="0 0 64 64" aria-hidden="true">
        <ellipse class="product-image__shape" cx="32" cy="32" rx="16" ry="23" transform="rotate(30 32 32)"/>
        <path class="product-image__line" d="M24 13c11 10 3 24 16 38"/>
    </svg>
`;

const CUP = `
    <svg viewBox="0 0 64 64" aria-hidden="true">
        <path class="product-image__shape" d="M12 22h32v13a15 15 0 0 1-15 15h-2a15 15 0 0 1-15-15z"/>
        <path class="product-image__line" d="M44 26h3a7 7 0 0 1 0 14h-4"/>
        <path class="product-image__line" d="M20 9c-2 3 2 5 0 8M28 9c-2 3 2 5 0 8M36 9c-2 3 2 5 0 8"/>
    </svg>
`;

export function createProductImage({ product }) {
    const element = document.createElement("div");

    element.className = "product-image";
    // The ProductImage.js section in style.css picks the colours: LIGHT, MEDIUM, DARK or NONE (equipment)
    element.dataset.roast = product.roastLevel ?? "NONE";
    element.innerHTML = product.roastLevel ? BEAN : CUP;

    return element;
}
