# Frontend

Plain JavaScript modules, loaded by the browser as they are: no npm and no build step. Nginx serves
`frontend/src` straight from your folder, so edit, save and reload the browser.

The browser only talks to Caddy on port 8088, so the pages and the API share one origin: **no CORS
configuration**. The frontend calls relative URLs like `/api/products`.

```
frontend/src/
├── index.html, style.css   header and nav, <main id="app">; one CSS section per component
├── app.js                  init() and the routes: path -> page
├── router.js               render(Page, props), Navigo, setTitle()
├── auth.js                 who is logged in (GET /api/me), subscribeToUser()
├── guards.js               requireAuth, requireRole, requireGuest for the routes
├── cart.js                 the cart of this browser, subscribeToCart()
├── session.js              the guest cart's id in localStorage
├── services/               http.js + one module per backend feature
├── pages/                  XxxPage(props) returns { element, mount, unmount }
└── components/             createXxx({ ...options, onSomething }) returns an element
```

- **Every request goes through `services/http.js`.** It sends the CSRF token, and throws an
  `ApiError` with the ProblemDetail `detail` and the field `errors`.
- **Pages** return their element right away ("Loading...") and load their data in `mount()`.
- **Components never call the API.** They report through callbacks (`onAddToCart`), and the page
  calls the API and redraws.
- **Data goes in with `textContent`**, never inside an HTML string.
- The folder is `services/`, not `api/`: Caddy sends every `/api/*` request to Spring Boot.
