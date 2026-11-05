# Coffee Shop

A small web shop for coffee beans and brewing gear. Customers browse the catalog, fill a cart,
check out and follow their orders.

It is a **reference app**: a complete, working base that is built to be extended. Every feature
follows the same pattern, so a new one (reviews, wishlist, discount codes, login, ...) is added
by copying what is already there. See [Adding a feature](#adding-a-feature).

Diagrams of the packages, the database, the checkout flow and the implemented user stories are in
[ARCHITECTURE.md](ARCHITECTURE.md).

| Part | What it is |
|------|------------|
| `coffeeshop-api/` | Spring Boot REST API: Java 25, Maven, Spring Data JPA, MySQL |
| `frontend/` | Single-page app in plain JavaScript (no Node, no build step), Navigo router, served by Nginx |
| `Caddyfile` | Reverse proxy: one address for both the pages and the API |

## Contents

- [Features](#features)
- [Technology](#technology)
- [Prerequisites](#prerequisites)
- [Repository structure](#repository-structure)
- [Run it](#run-it)
- [How the parts fit together](#how-the-parts-fit-together)
- [Backend architecture](#backend-architecture)
- [REST API](#rest-api)
- [Frontend](#frontend)
- [Tests](#tests)
- [CI and releases](#ci-and-releases)
- [Deploy to Azure](#deploy-to-azure)
- [Adding a feature](#adding-a-feature)
- [Feature ideas](#feature-ideas)
- [Known limitations](#known-limitations)
- [Troubleshooting](#troubleshooting)

## Features

- **Catalog**: categories and products (beans, ground coffee, equipment), filtering by category and search by name or origin
- **Customers**: a profile with name, email and delivery address
- **Cart**: stored on the server, so it survives a reload; stock is checked when adding
- **Orders**: checkout turns a cart into an order, takes the items out of stock and empties the cart; orders can be cancelled (stock goes back) and shipped

There is **no login yet**: the browser remembers its cart and customer in `localStorage`.
Adding Spring Security is one of the [feature ideas](#feature-ideas).

## Technology

| | |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator), Maven |
| Database | MySQL 8.4 (Hibernate creates the tables) |
| Frontend | HTML, CSS and JavaScript modules, [Navigo 8.11.1](https://github.com/krasimir/navigo) (copied into `src/vendor/`) |
| Servers | Caddy 2.11 (reverse proxy), Nginx 1.30 (static files) |
| Tests | JUnit 5, Mockito, H2 (in-memory database), ArchUnit |
| CI and releases | GitHub Actions, GitHub Container Registry (GHCR) |

## Prerequisites

- JDK 25 and IntelliJ IDEA (or any IDE). Maven is included as `./mvnw`.
- Docker Desktop (or Docker Engine with Compose)

## Repository structure

```
coffeeshop/
├── .github/workflows/
│   ├── ci.yml            tests and builds on every push and pull request
│   └── release.yml       publishes both images to GHCR for every version tag
├── compose.dev.yaml      db + frontend + caddy: Spring Boot runs in your IDE
├── compose.full.yaml     db + api + frontend + caddy: everything in Docker
├── Caddyfile             /api/* -> Spring Boot, everything else -> Nginx
├── .env.example          copy to .env (passwords)
├── docs/                 guides: deploy to Azure on one VM, or on two VMs in a private network
├── deploy/               the files the guides put on the VMs
│   ├── cloud-init.yaml   installs Docker on a new VM
│   ├── one-vm/           compose.yaml, Caddyfile, .env.example for everything on one VM
│   └── two-vms/          app/ and db/: the same, split over two VMs
├── coffeeshop-api/       Spring Boot (own Dockerfile -> image coffeeshop-api)
│   └── src/main/java/ek/osnb/demo/coffeeshop/
│       ├── common/       exceptions + ProblemDetail error handler, shared by all features
│       ├── catalog/      products and categories
│       ├── customer/     customer profiles
│       ├── cart/         shopping carts
│       └── order/        checkout and orders
└── frontend/             (own Dockerfile -> image coffeeshop-frontend)
    ├── Dockerfile, nginx/default.conf
    ├── scripts/update-navigo.sh
    └── src/
        ├── index.html, style.css, favicon.svg
        ├── app.js        init() + routes: path -> page
        ├── router.js     render(Page, props) + Navigo
        ├── cart.js       the cart: state + subscribeToCart()
        ├── session.js    cart id and customer id in localStorage
        ├── format.js     prices, dates
        ├── services/     http.js + one module per backend feature
        ├── pages/        one XxxPage.js per page, ErrorPages.js
        ├── components/   createXxx() components
        └── vendor/       navigo.js (downloaded, don't edit)
```

## Run it

Create your `.env` once:

```bash
cp .env.example .env      # then replace the two passwords
```

### Development: Spring Boot in your IDE

```bash
docker compose -f compose.dev.yaml up -d
```

Then start `CoffeeShopApplication` in IntelliJ (or `cd coffeeshop-api && ./mvnw spring-boot:run`)
and open **http://localhost:8088**.

- Spring Boot reads the passwords from `.env` itself (`spring.config.import` in `application.properties`).
- Nginx serves `frontend/src` from your working copy: edit, save, reload the browser. No image to build.
- MySQL is on `localhost:3306` for Spring Boot and for your database tool.
- The catalog is filled by `CatalogInitData` on the first start.

### Everything in Docker

```bash
docker compose -f compose.full.yaml up -d --build
```

Open **http://localhost:8088**. Use `--build` again after changing code.

### Stop

With the file you started with:

```bash
docker compose -f compose.dev.yaml down       # keeps the data
docker compose -f compose.dev.yaml down -v    # also deletes the database
```

Both files use the project name `coffeeshop`, so dev and full share the same database.

### Build the images separately

The backend and the frontend are separate images, built from their own folder:

```bash
docker build -t coffeeshop-api ./coffeeshop-api
docker build -t coffeeshop-frontend ./frontend
```

## How the parts fit together

```
                       ┌────────────────────────────┐
 browser ── :8088 ───▶ │ Caddy                      │
                       │  /api/*   ──▶ Spring Boot  │──▶ MySQL
                       │  others   ──▶ Nginx        │
                       └────────────────────────────┘
```

- The browser only talks to Caddy, so pages and API share one origin: **no CORS configuration**.
  The frontend calls relative URLs like `/api/products`.
- Nginx serves the files in `frontend/src`. For paths without a file (e.g. `/orders/3`) it
  answers with `index.html`, and Navigo shows the right page. That is why `index.html` uses
  absolute paths such as `/style.css`.
- Spring Boot only serves JSON under `/api`.

## Backend architecture

### Packaged by feature

Each feature is one package containing everything it needs: entity, repository, service,
controller, request and response records.

```
catalog            customer
   ▲                   ▲
   │                   │
  cart ◀──────────── order
   (cart → catalog,  order → catalog, customer, cart)
```

The arrows show who uses whom. `catalog` and `customer` use nobody, and nothing uses `order`.

### The rules

These are checked on every build by `ArchitectureTest` (ArchUnit). A new feature package is checked automatically.

1. **A feature only shows its contract.** Everything is package-private except:
   - `XxxApi`: the interface other features call, e.g. `CatalogApi`
   - `XxxInfo`: the read-only records it returns, e.g. `ProductInfo`
   - `XxxEvent`: events it publishes, e.g. `OrderPlacedEvent`
   - `XxxException`: exceptions others may meet, e.g. `InsufficientStockException`

   So the compiler stops `cart` from using `Product` or `ProductRepository`.
2. **Across features: ids, not entities.** `CartItem` stores `Long productId`, not
   `@ManyToOne Product`. Inside a feature, JPA associations are fine (`Product` → `Category`,
   `Cart` → `CartItem`, `Order` → `OrderLine`).
3. **No cycles between features.** If two features need each other, one publishes an event instead.
4. **`common` knows no feature.** It holds the base exceptions and the error handler.
5. **Copy what must not change.** An `OrderLine` keeps the product name and price, and the order keeps
   the shipping address, as they were when the order was placed. The cart doesn't copy anything:
   it shows today's prices from `CatalogApi`.

### `XxxApi` or an event?

| Use | When | Example |
|-----|------|---------|
| Call another feature's `XxxApi` | You need an answer, or it must happen in the same transaction | Order asks `CatalogApi.reserveStock()`; if stock is short, the whole order is rolled back |
| Publish an event | Something happened and others *may* react; the publisher shouldn't know them | `OrderService` publishes `OrderPlacedEvent`; a future loyalty feature listens |

### Inside a feature

The pattern, shown for `cart`:

| Class | Role |
|-------|------|
| `Cart`, `CartItem` | JPA entities with behaviour (`addItem`, `changeQuantity`), no setters. They guard their own rules. |
| `CartRepository` | `JpaRepository<Cart, Long>` |
| `CartService` | `@Service @Transactional`, implements the public `CartApi` |
| `CartController` | `@RestController` under `/api/carts` |
| `AddCartItemRequest`, `CartResponse`, ... | records; requests use Bean Validation (`@NotNull`, `@Min`) |
| `CartApi`, `CartInfo`, `CartLineInfo` | the public contract (see rule 1) |

### Errors

Every error is a [ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`),
created by `common/ApiExceptionHandler`:

```json
{ "title": "Conflict", "status": 409,
  "detail": "Only 3 left of French Roast", "instance": "/api/carts/1/items" }
```

| Status | Thrown as | Example |
|--------|-----------|---------|
| 400 | Bean Validation (`@Valid`), plus an `errors` object per field | `{"errors": {"email": "must be a well-formed email address"}}` |
| 400 | `InvalidInputException` | Quantity 0 |
| 404 | `NotFoundException` | `Product not found: 999` |
| 409 | `ConflictException` (and subclasses) | Not enough stock, email already used, cancelling a shipped order |

A feature throws one of these (or its own subclass, like `InsufficientStockException extends ConflictException`), so it never needs its own handler.

## REST API

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/categories` | | All categories |
| GET | `/api/products?categoryId=&q=` | | Products; both filters optional, `q` searches name and origin |
| GET | `/api/products/{id}` | | One product |
| POST | `/api/customers` | `{name, email, street, postalCode, city}` | Create a customer (409 if the email is used) |
| GET | `/api/customers/{id}` | | One customer |
| GET | `/api/customers?email=` | | 0 or 1 customer with that email (stand-in for login) |
| PUT | `/api/customers/{id}` | same as POST | Update a customer |
| POST | `/api/carts` | | Create an empty cart |
| GET | `/api/carts/{id}` | | The cart with live names, prices and totals |
| POST | `/api/carts/{id}/items` | `{productId, quantity}` | Add a product (adds to the quantity if it's already there) |
| PUT | `/api/carts/{id}/items/{productId}` | `{quantity}` | Change the quantity |
| DELETE | `/api/carts/{id}/items/{productId}` | | Remove a product |
| POST | `/api/orders` | `{customerId, cartId}` | Checkout: create an order from the cart |
| GET | `/api/orders?customerId=` | | The customer's orders, newest first |
| GET | `/api/orders/{id}` | | One order with lines and address |
| POST | `/api/orders/{id}/cancel` | | PLACED → CANCELLED, stock goes back |
| POST | `/api/orders/{id}/ship` | | PLACED → SHIPPED (staff only, once there is a login) |

Every cart change returns the whole updated cart.

### Try it with curl

```bash
API=http://localhost:8088/api

curl -s $API/products?q=kenya

curl -s -X POST $API/customers -H 'Content-Type: application/json' \
  -d '{"name":"Anna Jensen","email":"anna@example.com","street":"Nørregade 1","postalCode":"1165","city":"København K"}'

curl -s -X POST $API/carts
curl -s -X POST $API/carts/1/items -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -s -X POST $API/orders -H 'Content-Type: application/json' -d '{"customerId":1,"cartId":1}'
curl -s -X POST $API/orders/1/ship
```

## Frontend

Plain JavaScript modules, loaded by the browser as they are. There is no `npm` and no build step.

| File | Role |
|------|------|
| `index.html` | Header with nav and cart badge, `<main id="app">`, import map `"navigo" → /vendor/navigo.js` |
| `app.js` | `init()`: starts the cart badge, loads the cart (or shows `ServerDownPage`), then the route list: `router.on("/cart", () => render(CartPage))` |
| `router.js` | `render(Page, props)` swaps the page in `<main>`; Navigo in history mode; `setTitle()`; an `after` hook (active nav link, scroll); one click listener for all `data-navigo` links |
| `services/http.js` | `getJSON` / `sendJSON`; throws `ApiError` with the ProblemDetail `detail` and field `errors` |
| `services/*.js` | One module per backend feature (`catalog.js`, `carts.js`, `customers.js`, `orders.js`) |
| `cart.js` | The cart of this browser: `refreshCart`, `addToCart`, `changeQuantity`, `removeFromCart`, `getCurrentCart`, `subscribeToCart` |
| `session.js` | Cart id and customer id in `localStorage` (`getCartId`, `loadCustomer`, `setCustomerId`) |
| `pages/*.js` | `XxxPage(props)` returns `{ element, mount, unmount }` (or just an element); `mount()` sets the title and loads the data |
| `components/*.js` | `createXxx({...options, onSomething})` returns an element (or `{ element, ...methods }`) |

Conventions:

- **Pages** return their element right away, showing "Loading...", and fill it in `mount()` once the
  data is there. `render()` calls `unmount()` on the old page first, e.g. to stop a timer.
- **Components** build a fixed HTML skeleton with `innerHTML` and fill in data with `textContent`,
  never by putting data into HTML. Their CSS is a section in `style.css`, named after the component
  (`/* CartLine.js */`), with BEM class names, e.g. `.cart-line__total`.
- **`services/`, not `api/`**: Caddy sends every `/api/*` request to Spring Boot, so a file served
  as `/api/carts.js` would never reach Nginx.
- **Components don't call the API.** They report what happened through callbacks (`onAddToCart`,
  `onQuantityChange`), and the page calls the API and redraws.
- **The cart** lives in `cart.js`. Pages change it through `cart.js`, never through `services/carts.js`,
  and the badge in the header redraws itself through `subscribeToCart()`, so no page has to update it.
- **Colours** are tokens in `:root` (`--accent`, `--surface`, ...), with a dark-mode set.

Update Navigo with `frontend/scripts/update-navigo.sh 8.11.1` (or a newer version).

## Tests

```bash
cd coffeeshop-api
./mvnw test      # all tests - no MySQL and no Docker needed
```

| Test | Kind | What it shows |
|------|------|---------------|
| `ProductTest`, `CartTest`, `OrderTest` | Plain JUnit | The entities' rules: stock, quantities, order status changes |
| `CartServiceTest`, `OrderServiceTest` | Mockito | A service tested alone. The other features are mocked through their `XxxApi`, which works because features only know each other by id and interface |
| `ArchitectureTest` | ArchUnit | The [architecture rules](#the-rules) |
| `CheckoutFlowTest` | `@SpringBootTest` + MockMvc + H2 | The whole API with the real application: checkout, stock, cancelling, ProblemDetail errors, the published event |

The tests use **H2**, an in-memory database, instead of MySQL. `src/test/resources/application.properties`
has the same name as the one in `src/main/resources`, so the tests use it instead: it points the
datasource at H2 (in MySQL mode) and creates fresh tables for every run.

H2 is not MySQL, so a few things can only go wrong in production, for example SQL that H2 accepts
but MySQL doesn't. Running the app in Docker before a release catches those.

## CI and releases

GitHub Actions runs two workflows from `.github/workflows/`. They only work when `coffeeshop/` is
the root of the Git repository, because GitHub only looks for `.github/` there.

| Workflow | Starts on | Does |
|---|---|---|
| `ci.yml` | Every push to a branch, every pull request | `./mvnw verify` (all tests, then the jar), checks both Compose setups, builds both images. It publishes nothing |
| `release.yml` | Pushing a tag like `v1.0.0` | The tests again, then pushes both images to GHCR and creates a GitHub release |

### Versions

A **release** is a version you can point at later: "the server runs 1.2.0". Versions follow
*semantic versioning*, `MAJOR.MINOR.PATCH`:

| Change | Example | When |
|---|---|---|
| PATCH | `1.0.0` → `1.0.1` | A bug fix, nothing new |
| MINOR | `1.0.1` → `1.1.0` | A new feature (e.g. reviews); everything that worked still works |
| MAJOR | `1.1.0` → `2.0.0` | Something that worked before now works differently (e.g. login becomes required) |

A version is marked with a **Git tag**: a name that points at one commit and, unlike a branch, never moves.

```bash
git tag -a v1.0.0 -m "First release"     # tag the current commit (annotated tag)
git push origin v1.0.0                   # tags are not pushed by a plain "git push"
```

### The images on GHCR

Pushing `v1.2.3` publishes:

```text
ghcr.io/<owner>/<repository>-api:1.2.3        also tagged 1.2 and latest
ghcr.io/<owner>/<repository>-frontend:1.2.3   also tagged 1.2 and latest
```

`<owner>/<repository>` is your GitHub user (or organisation) and repository, in lower case. For a
repository called `coffeeshop`, that gives `ghcr.io/<owner>/coffeeshop-api` and
`ghcr.io/<owner>/coffeeshop-frontend`. The images appear under *Packages* on your GitHub profile, and
the release under *Releases* in the repository, with notes listing the changes since the last one.

- The workflows use the built-in `GITHUB_TOKEN`, with only the permissions they need
  (`contents: read` for CI; `contents: write` and `packages: write` for a release).
  No secrets of your own are needed: the Compose check uses placeholder passwords.
- Every action is pinned to a commit SHA, with the version in a comment.
- **Publishing is not deploying.** A release puts images in a registry; nothing starts them anywhere.
  Running a version means pulling its images, for example:

  ```bash
  docker pull ghcr.io/<owner>/coffeeshop-api:1.0.0
  ```

  New packages on GHCR are private. To pull one without `docker login ghcr.io`, make it public in the
  package's settings on GitHub.

## Deploy to Azure

Two step-by-step guides put a release on Azure virtual machines, using only the Azure CLI (`az`) and
SSH. The VMs download the images from GHCR, so nothing is built on them.

| Guide | Result |
|-------|--------|
| [Part 1: everything on one VM](docs/01-azure-one-vm.md) | One `Standard_B1ms` VM (1 vCPU, 2 GB) runs MySQL, Spring Boot, Nginx and Caddy, like `compose.full.yaml`. Firewall, DNS name, HTTPS with Let's Encrypt, new versions |
| [Part 2: app and database on separate VMs](docs/02-azure-two-vms.md) | `vm-app` (Caddy, frontend, api) and `vm-db` (MySQL, no public IP) in one virtual network. Only the app subnet can reach port 3306 |

The files the guides copy to the VMs are in [`deploy/`](deploy/): a `cloud-init.yaml` that installs
Docker, and a `compose.yaml`, `Caddyfile` and `.env.example` for each setup.

## Adding a feature

Example: **product reviews**. A customer gives a product 1-5 stars and a comment.

### Backend checklist

1. **Create a package**: `ek.osnb.demo.coffeeshop.review`.
2. **Entity**: refer to other features by id.
   ```java
   @Entity
   @Table(name = "reviews")
   class Review {
       @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(nullable = false)
       private Long productId;      // catalog - by id, no @ManyToOne Product

       @Column(nullable = false)
       private Long customerId;     // customer - by id

       @Column(nullable = false)
       private int rating;

       @Column(length = 1000)
       private String comment;

       protected Review() { }
       // static create(...) that checks 1 <= rating <= 5 (throw InvalidInputException)
   }
   ```
3. **Repository**: `interface ReviewRepository extends JpaRepository<Review, Long>` with
   `List<Review> findByProductIdOrderByIdDesc(Long productId)`.
4. **Service**: check the ids through the other features' APIs. They throw `NotFoundException`
   (404) for unknown ids.
   ```java
   @Service
   @Transactional
   class ReviewService {
       private final ReviewRepository repository;
       private final CatalogApi catalog;
       private final CustomerApi customers;
       // constructor ...

       ReviewResponse create(Long productId, CreateReviewRequest request) {
           catalog.getProduct(productId);               // 404 if the product doesn't exist
           CustomerInfo customer = customers.getCustomer(request.customerId());
           Review review = repository.save(Review.create(productId, customer.id(), request.rating(), request.comment()));
           return ReviewMapper.toResponse(review, customer.name());
       }
   }
   ```
5. **Controller and records**: `ReviewController` with `GET` and `POST /api/products/{productId}/reviews`,
   plus `CreateReviewRequest` (`@Min(1) @Max(5) int rating`, ...) and `ReviewResponse`.
6. **Contract, only if needed**: if another feature needs reviews later (e.g. the catalog shows the average
   rating), add `ReviewApi` + `ReviewInfo`. Watch rule 3: if `review` already uses `catalog`,
   `catalog` cannot use `review`. In that case the frontend combines the two, or the catalog gets the average from an event.
7. **Tests**: `ReviewTest` (rating rules), `ReviewServiceTest` (mock `CatalogApi`, `CustomerApi`),
   and a case in `CheckoutFlowTest` (or a `ReviewFlowTest` of its own). Run `./mvnw test`:
   `ArchitectureTest` checks the new package too.

There is no SQL to write: Hibernate (`ddl-auto=update`) creates the `reviews` table on the next start.

### Frontend checklist

1. **API module** `services/reviews.js`:
   ```js
   import { getJSON, sendJSON } from "./http.js";

   export function getReviews(productId) {
       return getJSON(`/api/products/${productId}/reviews`);
   }

   export function createReview(productId, { customerId, rating, comment }) {
       return sendJSON("POST", `/api/products/${productId}/reviews`, { customerId, rating, comment });
   }
   ```
2. **Components** `components/ReviewList.js` and `ReviewForm.js`, following `CustomerForm.js`: an
   `onSubmit(data)` that returns a promise. Add their CSS as `/* ReviewList.js */` and `/* ReviewForm.js */`
   sections in the COMPONENTS part of `style.css`.
3. **Use them** in `pages/ProductPage.js`: load `getReviews(product.id)` next to the product, append the
   list, and show the form when `getCustomerId()` (from `session.js`) is set.
4. **A page of its own?** Write `pages/ReviewsPage.js` like the other pages (`{ element, mount }`, `setTitle("Reviews")`
   in `mount`), add a route in `app.js` (`.on("/reviews", () => render(ReviewsPage))`)
   and, if needed, a link in the `<nav>` in `index.html`.

## Feature ideas

| Feature | Touches | Good for practising |
|---------|---------|---------------------|
| Reviews and ratings | new `review` feature, product page | The full checklist above |
| Wishlist | new `wishlist` feature (`customerId` + `productId`s) | id references, a new page |
| Discount codes | new `discount` feature, called by `order` at checkout | `XxxApi`, snapshots (save the discount on the order) |
| Loyalty points | new `loyalty` feature listening to `OrderPlacedEvent` | Events: `@TransactionalEventListener`, no change to `order` |
| Low-stock alert | listens to stock changes, or a scheduled check | Events or `@Scheduled` |
| Login with Spring Security | customer becomes the logged-in user; cart and orders tied to it; `ship` only for staff | Security, roles, removing `?email=` and `customerId` from requests |
| Product management (admin) | POST/PUT/DELETE in `catalog`, admin pages | Validation, roles |
| Coffee recommendations with AI | new feature using Spring AI with the catalog through `CatalogApi` | Spring AI, prompts with real data |
| Product photos | `imageUrl` on `Product`, `ProductImage.js` shows it | Small change across backend and frontend |
| Continuous deployment | a workflow job that runs `docker compose pull && docker compose up -d` on the VM over SSH after every release (see [Deploy to Azure](#deploy-to-azure)) | GitHub Actions, secrets, SSH keys |

## Known limitations

These are deliberate, to keep the base small. Most of them are good features to add.

- **No login.** Anyone who knows (or guesses) an id can read that cart, customer or order, and
  `GET /api/customers?email=` works as a "login" with just an email. Spring Security fixes this.
- **`/api/orders/{id}/ship` is open** to everyone. It should be a staff-only action.
- **Ids are sequential** (`Long`, `IDENTITY`), so they are easy to guess.
- **Old carts are never deleted.**
- **`ddl-auto=update`** adds tables and columns, but never removes or renames them. A real project
  would use migrations (Flyway).

## Troubleshooting

| Problem | Fix |
|---------|-----|
| A page shows "Cannot reach the API (HTTP 502). Is Spring Boot running?" | Dev mode: start `CoffeeShopApplication`. Full mode: `docker compose -f compose.full.yaml ps` |
| Spring Boot: `Access denied for user` | `.env` is missing or its password differs from the one MySQL was created with. Fix `.env`, or start over with `down -v` |
| Linux: Caddy can't reach Spring Boot in your IDE | `host.docker.internal` comes from `extra_hosts` in `compose.dev.yaml`. Also check that your firewall allows Docker to reach port 8080 |
| You renamed a field and the old column is still there | `ddl-auto=update` never drops columns: `down -v` and start again |
| The cart or profile is "lost" after `down -v` | The browser still has the old ids in `localStorage`. The app notices the 404 and starts over |
| GitHub Actions: `./mvnw: Permission denied` | The file lost its executable bit (often after a commit from Windows): `git update-index --chmod=+x coffeeshop-api/mvnw`, then commit |
| The workflows never start on GitHub | `.github/` must be at the root of the repository, not inside a subfolder |
| `docker pull ghcr.io/...` says `denied` or `unauthorized` | The package is private: `docker login ghcr.io`, or make the package public in its settings |
| The release fails when pushing an image (`permission_denied`) | A package with that name already belongs to another repository. In the package's settings, give this repository *Write* access under *Manage Actions access* |
