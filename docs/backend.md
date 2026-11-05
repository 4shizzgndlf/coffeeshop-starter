# Backend

Spring Boot only serves JSON under `/api`. `CatalogInitData` fills the catalog on the first start.
MySQL runs in Docker on `localhost:3306`, for Spring Boot and for your database tool.

## Packaged by feature

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

## Errors

Errors are [ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457) JSON, made by
`common/ApiExceptionHandler`. Throw `NotFoundException` (404), `ConflictException` (409) or
`InvalidInputException` (400). A failed `@Valid` gives 400 with an `errors` object per field:

```json
{ "title": "Conflict", "status": 409,
  "detail": "Only 3 left of French Roast", "instance": "/api/carts/1/items" }
```

## REST API

The backend as it is now. The frontend already uses the API in [AI-PROJECT.md](../AI-PROJECT.md),
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

## Tests

| Test | Kind | What it shows |
|------|------|---------------|
| `ProductTest`, `CartTest`, `OrderTest` | Plain JUnit | The entities' rules |
| `CartServiceTest`, `OrderServiceTest` | Mockito | A service alone, with the other features mocked through their `XxxApi` |
| `CatalogApiTest`, `CustomerApiTest`, `OrderApiTest` | `@SpringBootTest` | The read-only Apis that AI tools can call |
| `CheckoutFlowTest` | `@SpringBootTest` + MockMvc | The whole API, from catalog to cancelled order |
| `ArchitectureTest` | ArchUnit | The rules above |

The tests use H2, an in-memory database, set up in `src/test/resources/application.properties`.

## Known limitations

- **No login.** Anyone who knows an id can read that cart, customer or order, and
  `GET /api/customers?email=` works as a "login" with just an email. Fixing this is the AI project.
- **`/api/orders/{id}/ship` is open** to everyone.
- **Ids are sequential**, so they are easy to guess.
- **`ddl-auto=update`** never removes or renames columns. A real project would use migrations (Flyway).
