import Navigo from "navigo";

const APP_NAME = "Coffee Shop";
const app = document.querySelector("#app");

let currentPage = null;

export function render(Page, props = {}) {
    // 1. Create new page
    const result = Page(props);

    // 2. Normalize page to support both DOM elements
    //    and objects with lifecycle methods
    const nextPage = result instanceof Node
        ? { element: result }
        : result;

    // 3. Validate that the page contains a DOM element
    if (!(nextPage?.element instanceof Node)) {
        throw new Error("Page must return a DOM node or page object");
    }

    // 4. Clean up previous page
    currentPage?.unmount?.();

    // 5. Replace previous page with new page
    app.replaceChildren(nextPage.element);

    // 6. Track current page
    currentPage = nextPage;

    // 7. Run lifecycle method after mounting
    nextPage.mount?.();
}

export const router = new Navigo("/");

// Runs after every route, also notFound: highlight the nav link and start at the top.
// Navigo copies these hooks into each route when the route is added, so this has to run
// before app.js adds its routes - it does, because app.js imports this file first.
router.hooks({
    after(match) {
        // Navigo runs this before it updates the URL, so use the matched path
        markCurrentNavLink(`/${match.url}`);
        window.scrollTo(0, 0);
    }
});

// Sets the browser tab title, e.g. "Cart · Coffee Shop". Each page calls it in mount().
export function setTitle(title) {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
}

// Navigo only sets up data-navigo links after a route handler. Pages add most of their
// links later, after loading data, so this one listener handles every link instead.
// Links Navigo did set up (the header) are skipped: Navigo already called preventDefault.
document.addEventListener("click", (event) => {
    const link = event.target.closest('a[data-navigo]:not([data-navigo="false"]):not([target="_blank"])');
    const isPlainClick = event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey;

    if (!link || !isPlainClick || event.defaultPrevented) {
        return;
    }

    event.preventDefault();
    router.navigate(link.getAttribute("href"));
});

// Highlights the nav link for the current page ("/" exact, others by prefix)
function markCurrentNavLink(path) {
    for (const link of document.querySelectorAll("nav a")) {
        const href = link.getAttribute("href");
        const isCurrent = href === "/" ? path === "/" : path === href || path.startsWith(`${href}/`);

        if (isCurrent) {
            link.setAttribute("aria-current", "page");
        } else {
            link.removeAttribute("aria-current");
        }
    }
}
