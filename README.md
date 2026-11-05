# Coffee Shop

A small web shop for coffee beans and brewing gear. Customers browse the catalog, fill a cart,
check out and follow their orders.

It is a complete, working app that is built to be extended. Every feature follows the same pattern,
so you add a new one by copying what is already there.

> **AI project:** start with [AI-PROJECT.md](AI-PROJECT.md). The frontend already uses the secured
> API described there; the backend is yours to change.

| Part | What it is |
|------|------------|
| `coffeeshop-api/` | REST API: Java 25, Spring Boot 4.1, Spring Data JPA, MySQL 8.4 |
| `frontend/` | Single-page app in plain JavaScript modules (no npm, no build step), Navigo router, served by Nginx |
| `Caddyfile` | Reverse proxy: one address for both the pages and the API |
| `compose.dev.yaml`, `compose.full.yaml` | Docker Compose: `dev` runs MySQL, Nginx and Caddy; `full` also runs Spring Boot |
| `.github/workflows/ci.yml` | Runs the tests and builds both images on every push |

Diagrams of the packages, the database and the checkout flow are in [ARCHITECTURE.md](ARCHITECTURE.md).

## Run it

You need JDK 25, an IDE (e.g. IntelliJ IDEA) and Docker Desktop. Maven is included as `./mvnw`.

```bash
cp .env.example .env                        # once, then replace the two passwords
docker compose -f compose.dev.yaml up -d    # MySQL, Nginx and Caddy
```

Then start `CoffeeShopApplication` in your IDE (or `cd coffeeshop-api && ./mvnw spring-boot:run`)
and open **http://localhost:8088**. Nginx serves `frontend/src` straight from your folder: edit,
save, reload the browser.

| To | Run |
|----|-----|
| Run everything in Docker | `docker compose -f compose.full.yaml up -d --build` (`--build` again after changing code) |
| Stop | `docker compose -f compose.dev.yaml down` (add `-v` to delete the database too) |
| Run the tests | `cd coffeeshop-api && ./mvnw test` (no MySQL or Docker needed) |

## How the parts fit together

```
                       ┌────────────────────────────┐
 browser ── :8088 ───▶ │ Caddy                      │
                       │  /api/*   ──▶ Spring Boot  │──▶ MySQL
                       │  others   ──▶ Nginx        │
                       └────────────────────────────┘
```

- The browser only talks to Caddy, so the pages and the API share one origin: **no CORS
  configuration**. The frontend calls relative URLs like `/api/products`.
- Spring Boot only serves JSON under `/api`. `CatalogInitData` fills the catalog on the first start.
- MySQL is on `localhost:3306`, for Spring Boot and for your database tool.

## Backend: packaged by feature

```
coffeeshop-api/src/main/java/ek/osnb/demo/coffeeshop/
├── common/      exceptions and the error handler, shared by all features
├── catalog/     products and categories
├── customer/    customer profiles
├── cart/        shopping carts          uses catalog
└── order/       checkout and orders     uses catalog, customer and cart
```

Each feature package holds everything it needs: entity, repository, service, controller, and
request/response records. `ArchitectureTest` (ArchUnit) checks these rules on every build:

1. **A feature only shows its contract.** Everything is package-private except `XxxApi` (the
   interface other features call, e.g. `CatalogApi`), `XxxInfo` (the read-only records it returns),
   `XxxEvent` and `XxxException`.
2. **Across features: ids, not entities.** `CartItem` stores `Long productId`, not `@ManyToOne Product`.
3. **No cycles between features.** If two features need each other, one publishes an event instead,
   like `OrderPlacedEvent`.
4. **`common` knows no feature.**

Entities guard their own rules (`cart.addItem(...)`, `order.cancel()`), and have no setters.

**Errors** are [ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457) JSON, made by
`common/ApiExceptionHandler`. Throw `NotFoundException` (404), `ConflictException` (409) or
`InvalidInputException` (400). A failed `@Valid` gives 400 with an `errors` object per field:

```json
{ "title": "Conflict", "status": 409,
  "detail": "Only 3 left of French Roast", "instance": "/api/carts/1/items" }
```

## REST API

The backend as it is now. The frontend already uses the API in [AI-PROJECT.md](AI-PROJECT.md),
so checkout, orders and the profile need your changes first.

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/categories` | | All categories |
| GET | `/api/products?categoryId=&q=` | | Products; both filters optional, `q` searches name and origin |
| GET | `/api/products/{id}` | | One product |
| POST | `/api/customers` | `{name, email, street, postalCode, city}` | Create a customer (409 if the email is used) |
| GET | `/api/customers/{id}` | | One customer |
| GET | `/api/customers?email=` | | 0 or 1 customer with that email (stand-in for a login) |
| PUT | `/api/customers/{id}` | same as POST | Update a customer |
| POST | `/api/carts` | | Create an empty cart |
| GET | `/api/carts/{id}` | | The cart with live names, prices and totals |
| POST | `/api/carts/{id}/items` | `{productId, quantity}` | Add a product |
| PUT | `/api/carts/{id}/items/{productId}` | `{quantity}` | Change the quantity |
| DELETE | `/api/carts/{id}/items/{productId}` | | Remove a product |
| POST | `/api/orders` | `{customerId, cartId}` | Checkout: turn the cart into an order |
| GET | `/api/orders?customerId=` | | The customer's orders, newest first |
| GET | `/api/orders/{id}` | | One order with lines and address |
| POST | `/api/orders/{id}/cancel` | | PLACED → CANCELLED, the stock goes back |
| POST | `/api/orders/{id}/ship` | | PLACED → SHIPPED (meant for staff) |

## Frontend

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

- **Every request goes through `services/http.js`.** It throws an `ApiError` with the ProblemDetail
  `detail` and the field `errors`.
- **Pages** return their element right away ("Loading...") and load their data in `mount()`.
- **Components never call the API.** They report through callbacks (`onAddToCart`), and the page
  calls the API and redraws.
- **Data goes in with `textContent`**, never inside an HTML string.
- The folder is `services/`, not `api/`: Caddy sends every `/api/*` request to Spring Boot.

## Tests

| Test | Kind | What it shows |
|------|------|---------------|
| `ProductTest`, `CartTest`, `OrderTest` | Plain JUnit | The entities' rules |
| `CartServiceTest`, `OrderServiceTest` | Mockito | A service alone, with the other features mocked through their `XxxApi` |
| `CheckoutFlowTest` | `@SpringBootTest` + MockMvc | The whole API, from catalog to cancelled order |
| `ArchitectureTest` | ArchUnit | The rules above |

The tests use H2, an in-memory database, set up in `src/test/resources/application.properties`.

## Adding a feature

Copy an existing feature. For example, product reviews:

1. A package `review` with an entity that stores `productId` and `customerId` (ids, not entities)
   and a `static create(...)` that checks its rules.
2. A repository, and a `@Service @Transactional` that checks the ids through `CatalogApi` and
   `CustomerApi` (they throw `NotFoundException` for unknown ids).
3. A controller with request and response records, using Bean Validation (`@Min(1) @Max(5)`).
4. A `ReviewApi` + `ReviewInfo` only if another feature needs reviews.
5. Tests. `./mvnw test` also checks the new package against the rules.
6. Frontend: `services/reviews.js`, components that report through callbacks, and a page (a route
   in `app.js` and a link in `index.html`) or a section on an existing page.

Hibernate (`ddl-auto=update`) creates the new table on the next start.

## Known limitations

- **No login.** Anyone who knows an id can read that cart, customer or order, and
  `GET /api/customers?email=` works as a "login" with just an email. Fixing this is the AI project.
- **`/api/orders/{id}/ship` is open** to everyone.
- **Ids are sequential**, so they are easy to guess.
- **`ddl-auto=update`** never removes or renames columns. A real project would use migrations (Flyway).

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "Cannot reach the API (HTTP 502). Is Spring Boot running?" | Start `CoffeeShopApplication`. In full mode: `docker compose -f compose.full.yaml ps` |
| Spring Boot: `Access denied for user` | `.env` is missing, or its password differs from the one MySQL was created with. Fix `.env`, or start over with `down -v` |
| Linux: Caddy can't reach Spring Boot in your IDE | Check that your firewall lets Docker reach port 8080 (`host.docker.internal` is set in `compose.dev.yaml`) |
| You renamed a field and the old column is still there | `ddl-auto=update` never drops columns: `down -v` and start again |
| GitHub Actions: `./mvnw: Permission denied` | `git update-index --chmod=+x coffeeshop-api/mvnw`, then commit |
