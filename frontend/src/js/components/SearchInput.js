// components/SearchInput.js

// A search field that calls onSearch(text) as the user types.
// While there's search text, setResultCount(shown, total) shows e.g. "Showing 2 of 5".
export function createSearchInput({
    label = "Search",
    placeholder = "Search...",
    onSearch
} = {}) {
    const element = document.createElement("div");

    element.className = "search-input";

    element.innerHTML = `
        <input class="search-input__field" type="search">
        <span class="search-input__count" aria-live="polite"></span>
      `;

    const input = element.querySelector(".search-input__field");
    const count = element.querySelector(".search-input__count");

    input.placeholder = placeholder;
    input.setAttribute("aria-label", label);

    input.addEventListener("input", () => {
        onSearch?.(input.value);
    });

    return {
        element,

        setResultCount(shown, total) {
            count.textContent = input.value.trim() ? `Showing ${shown} of ${total}` : "";
        }
    };
}
