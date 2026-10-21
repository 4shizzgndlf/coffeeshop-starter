import Navigo from "navigo";

export const router = new Navigo("/");

const APP_NAME = "Coffee Shop";
const app = document.querySelector("#app");

// Navigo only binds data-navigo links that exist when it runs. This one listener
// also handles links a page adds later, e.g. after loading data or re-rendering a list.
document.addEventListener("click", (event) => {
    const link = event.target.closest('a[data-navigo]:not([data-navigo="false"]):not([target="_blank"])');
    const isPlainClick = event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey;

    if (!link || !isPlainClick || event.defaultPrevented) {
        return;
    }

    event.preventDefault();
    router.navigate(link.getAttribute("href"));
});

// Wraps a page so every route renders the same way.
// A page is a function (container, params) that fills the container.
export function page(render, title) {
    return async (match) => {
        setTitle(title);
        // Navigo calls this before it updates the URL, so use the matched path
        markCurrentNavLink(`/${match.url}`);
        window.scrollTo(0, 0);

        await render(app, match.data ?? {});
    };
}

// Sets the browser tab title, e.g. "Cart · Coffee Shop"
export function setTitle(title) {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
}

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
