// format.js - turns values from the API into text for the page

const priceFormat = new Intl.NumberFormat("da-DK", {
    style: "currency",
    currency: "DKK"
});

const dateFormat = new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
});

// 119 -> "119,00 kr."
export function formatPrice(amount) {
    return priceFormat.format(Number(amount));
}

// "2026-10-20T09:15:00Z" -> "20 Oct 2026, 11:15" (in the browser's time zone)
export function formatDate(isoString) {
    return dateFormat.format(new Date(isoString));
}

// "MEDIUM" -> "Medium", for enum values from the API
export function formatEnum(value) {
    return value ? value.charAt(0) + value.slice(1).toLowerCase() : "";
}

// "Ethiopia · Light roast" for coffee, "" for equipment (no origin or roast level)
export function formatProductDetails(product) {
    const parts = [];

    if (product.origin) {
        parts.push(product.origin);
    }

    if (product.roastLevel) {
        parts.push(`${formatEnum(product.roastLevel)} roast`);
    }

    return parts.join(" · ");
}
