// components/CategoryFilter.js

// A row of buttons: "All" plus one per category. Calls onSelect(categoryId),
// with null for "All". aria-pressed marks the chosen one (and styles it).
export function createCategoryFilter({
    categories,
    selectedId = null,
    onSelect
}) {
    const element = document.createElement("div");

    element.className = "category-filter";
    element.setAttribute("role", "group");
    element.setAttribute("aria-label", "Category");

    const options = [{ id: null, name: "All" }, ...categories];

    for (const category of options) {
        const button = document.createElement("button");

        button.type = "button";
        button.className = "category-filter__option";
        button.textContent = category.name;
        button.setAttribute("aria-pressed", String(category.id === selectedId));

        button.addEventListener("click", () => {
            for (const other of element.querySelectorAll(".category-filter__option")) {
                other.setAttribute("aria-pressed", String(other === button));
            }

            onSelect?.(category.id);
        });

        element.append(button);
    }

    return element;
}
