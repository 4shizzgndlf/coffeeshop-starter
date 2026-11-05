# Coffee Shop: architecture and features

Diagrams of how the code is organised and what the app can do. The [README](README.md) explains
how to run it and how to add a feature.

GitHub, GitLab and IntelliJ (with the Mermaid plugin) render the diagrams below.

## Contents

1. [Runtime: the parts and how they talk](#1-runtime-the-parts-and-how-they-talk)
2. [Packages and their relationships](#2-packages-and-their-relationships)
3. [Inside each package](#3-inside-each-package)
   - [common](#common)
   - [catalog](#catalog)
   - [customer](#customer)
   - [cart](#cart)
   - [order](#order)
4. [Database: foreign keys inside a feature, ids across](#4-database-foreign-keys-inside-a-feature-ids-across)
5. [Checkout, step by step](#5-checkout-step-by-step)
6. [Frontend modules](#6-frontend-modules)
7. [Not built yet](#not-built-yet)

---

## 1. Runtime: the parts and how they talk

```mermaid
flowchart LR
    browser["Browser<br/>SPA: vanilla JS + Navigo"]
    caddy["Caddy<br/>reverse proxy, port 8088"]
    nginx["Nginx<br/>image: coffeeshop-frontend"]
    api["Spring Boot<br/>image: coffeeshop-api"]
    db[("MySQL 8.4")]

    browser -->|"every request"| caddy
    caddy -->|"/api/*"| api
    caddy -->|"everything else<br/>(falls back to index.html)"| nginx
    api -->|"JPA / JDBC"| db
```

The browser sees one address for both pages and API, so Spring Boot needs no CORS configuration.

---

## 2. Packages and their relationships

All Java code lives under `ek.osnb.demo.coffeeshop`. There is one package per feature, plus `common`.

```mermaid
flowchart TB
    order["order<br/>checkout and orders"]
    cart["cart<br/>shopping carts"]
    catalog["catalog<br/>products and categories"]
    customer["customer<br/>customer profiles"]
    common["common<br/>base exceptions + ProblemDetail error handler"]
    loyalty["loyalty<br/>NOT BUILT - example of a new feature"]

    order -->|"CatalogApi, ProductInfo"| catalog
    order -->|"CartApi, CartInfo, CartLineInfo"| cart
    order -->|"CustomerApi, CustomerInfo"| customer
    cart -->|"CatalogApi, ProductInfo,<br/>InsufficientStockException"| catalog

    loyalty -.->|"listens to OrderPlacedEvent"| order

    order -->|"throws"| common
    cart -->|"throws"| common
    catalog -->|"throws"| common
    customer -->|"throws"| common

    classDef feature fill:#f3e6dc,stroke:#8a4b2a,color:#2a211c
    classDef shared fill:#ece7e1,stroke:#75685e,color:#2a211c
    classDef future fill:#ffffff,stroke:#75685e,stroke-dasharray:5 5,color:#75685e
    class order,cart,catalog,customer feature
    class common shared
    class loyalty future
```

An arrow means "uses". The label says exactly which types are used. Only `CartService` and
`OrderService` cross a feature boundary, and only through these types.

### The public contract of each package

Every class is package-private except the ones below. `ArchitectureTest` checks this.

| Package | Public types (the contract) | Used by |
|---------|-----------------------------|---------|
| `catalog` | `CatalogApi`, `ProductInfo`, `InsufficientStockException` | `cart`, `order` |
| `customer` | `CustomerApi`, `CustomerInfo` | `order` |
| `cart` | `CartApi`, `CartInfo`, `CartLineInfo` | `order` |
| `order` | `OrderPlacedEvent` | nobody yet (an extension point) |
| `common` | `NotFoundException`, `ConflictException`, `InvalidInputException` | every feature |

### Rules shown by the diagram

- **Arrows point one way.** `catalog` and `customer` use no other feature, and nothing uses `order`,
  so there are no cycles.
- **Arrows end at a contract.** No arrow reaches an entity, repository or service in another package.
- **New features attach at the edge.** A future `loyalty` feature listens to `OrderPlacedEvent`.
  The order package doesn't change and doesn't know about it.

---

## 3. Inside each package

How to read the class diagrams:

| Notation | Meaning |
|----------|---------|
| `«public interface»`, `«public record»`, `«public»` | Part of the package's contract, usable from other packages |
| any other stereotype (`«Entity»`, `«Service»`, `«record»`, ...) | Package-private, so invisible outside the package |
| `+` method | public |
| `~` method | package-private |
| `-` field | private |
| `$` after a method | static |
| `A <|.. B` | B implements A |
| `A *-- B` | A owns B (JPA association inside the feature) |
| `A --> B` | A uses B |
| `A ..> B` | A creates, returns or throws B |
| a box in another namespace | a type from another package, i.e. a cross-package relationship |

### common

Base exceptions that every feature throws, and one handler that turns them into ProblemDetail
responses. `common` knows no feature: `InsufficientStockException` extends `ConflictException`, not
the other way round.

```mermaid
classDiagram
    direction LR

    namespace common {
        class ApiExceptionHandler {
            <<RestControllerAdvice>>
            ~notFound(NotFoundException) ProblemDetail
            ~conflict(ConflictException) ProblemDetail
            ~invalidInput(InvalidInputException) ProblemDetail
            ~concurrentChange(OptimisticLockingFailureException) ProblemDetail
            ~invalidRequest(MethodArgumentNotValidException) ProblemDetail
            ~unreadableBody(HttpMessageNotReadableException) ProblemDetail
            ~typeMismatch(MethodArgumentTypeMismatchException) ProblemDetail
        }
        class NotFoundException {
            <<public>>
        }
        class ConflictException {
            <<public>>
        }
        class InvalidInputException {
            <<public>>
        }
    }

    namespace catalog {
        class InsufficientStockException {
            <<public>>
        }
    }

    RuntimeException <|-- NotFoundException
    RuntimeException <|-- ConflictException
    RuntimeException <|-- InvalidInputException
    ConflictException <|-- InsufficientStockException

    ApiExceptionHandler ..> NotFoundException : 404
    ApiExceptionHandler ..> ConflictException : 409
    ApiExceptionHandler ..> InvalidInputException : 400
```

### catalog

Products and categories. It uses no other feature. Inside the package, `Product` has a JPA
association to `Category` (`@ManyToOne`), which is allowed because both belong to the catalog.

```mermaid
classDiagram
    direction TB

    namespace catalog {
        class CatalogController {
            <<RestController>>
            ~findCategories() List~CategoryResponse~
            ~search(Long categoryId, String q) List~ProductResponse~
            ~findById(Long id) ProductResponse
        }
        class CatalogApi {
            <<public interface>>
            +getProduct(Long productId) ProductInfo
            +findProducts(Collection~Long~ productIds) Map
            +reserveStock(Long productId, int quantity)
            +releaseStock(Long productId, int quantity)
        }
        class ProductService {
            <<Service>>
            ~findCategories() List~CategoryResponse~
            ~search(Long categoryId, String search) List~ProductResponse~
            ~findById(Long id) ProductResponse
        }
        class ProductInfo {
            <<public record>>
            +Long id
            +String name
            +BigDecimal price
            +int stock
        }
        class InsufficientStockException {
            <<public>>
        }
        class ProductRepository {
            <<JpaRepository>>
            ~search(Long categoryId, String search) List~Product~
        }
        class CategoryRepository {
            <<JpaRepository>>
        }
        class Product {
            <<Entity>>
            -Long id
            -String name
            -String description
            -Category category
            -String origin
            -RoastLevel roastLevel
            -BigDecimal price
            -int stock
            -Long version
            ~create(...)$ Product
            ~reserveStock(int quantity)
            ~releaseStock(int quantity)
        }
        class Category {
            <<Entity>>
            -Long id
            -String name
            ~create(String name)$ Category
        }
        class RoastLevel {
            <<enum>>
            LIGHT
            MEDIUM
            DARK
        }
        class CatalogMapper {
            ~toResponse(Product)$ ProductResponse
            ~toResponse(Category)$ CategoryResponse
            ~toInfo(Product)$ ProductInfo
        }
        class ProductResponse {
            <<record>>
        }
        class CategoryResponse {
            <<record>>
        }
        class CatalogInitData {
            <<CommandLineRunner>>
            +run(String... args)
        }
    }

    namespace common {
        class NotFoundException {
            <<public>>
        }
        class ConflictException {
            <<public>>
        }
        class InvalidInputException {
            <<public>>
        }
    }

    CatalogController --> ProductService
    CatalogApi <|.. ProductService
    CatalogApi ..> ProductInfo : returns
    ProductService --> ProductRepository
    ProductService --> CategoryRepository
    ProductService ..> CatalogMapper
    CatalogMapper ..> ProductResponse
    CatalogMapper ..> CategoryResponse
    CatalogMapper ..> ProductInfo
    ProductRepository ..> Product
    CategoryRepository ..> Category
    Product "*" --> "1" Category : @ManyToOne
    Product --> RoastLevel
    CatalogInitData --> ProductRepository
    CatalogInitData --> CategoryRepository

    Product ..> InsufficientStockException : throws
    ConflictException <|-- InsufficientStockException
    ProductService ..> NotFoundException : throws
    Product ..> InvalidInputException : throws
```

### customer

Customer profiles. It uses no other feature. Other features get a customer through
`CustomerApi.getCustomer(id)` as a `CustomerInfo` copy, never the `Customer` entity.

```mermaid
classDiagram
    direction TB

    namespace customer {
        class CustomerController {
            <<RestController>>
            ~create(CustomerRequest) CustomerResponse
            ~findById(Long id) CustomerResponse
            ~findByEmail(String email) List~CustomerResponse~
            ~update(Long id, CustomerRequest) CustomerResponse
        }
        class CustomerApi {
            <<public interface>>
            +getCustomer(Long customerId) CustomerInfo
        }
        class CustomerService {
            <<Service>>
            ~create(CustomerRequest) CustomerResponse
            ~findById(Long id) CustomerResponse
            ~findByEmail(String email) List~CustomerResponse~
            ~update(Long id, CustomerRequest) CustomerResponse
        }
        class CustomerInfo {
            <<public record>>
            +Long id
            +String name
            +String email
            +String street
            +String postalCode
            +String city
        }
        class CustomerRepository {
            <<JpaRepository>>
            ~findByEmail(String email) Optional~Customer~
            ~existsByEmail(String email) boolean
        }
        class Customer {
            <<Entity>>
            -Long id
            -String name
            -String email
            -String street
            -String postalCode
            -String city
            ~create(...)$ Customer
            ~updateDetails(...)
            ~normalizeEmail(String email)$ String
        }
        class CustomerMapper {
            ~toResponse(Customer)$ CustomerResponse
            ~toInfo(Customer)$ CustomerInfo
        }
        class CustomerRequest {
            <<record>>
        }
        class CustomerResponse {
            <<record>>
        }
    }

    namespace common {
        class NotFoundException {
            <<public>>
        }
        class ConflictException {
            <<public>>
        }
        class InvalidInputException {
            <<public>>
        }
    }

    CustomerController --> CustomerService
    CustomerController ..> CustomerRequest
    CustomerApi <|.. CustomerService
    CustomerApi ..> CustomerInfo : returns
    CustomerService --> CustomerRepository
    CustomerService ..> CustomerMapper
    CustomerMapper ..> CustomerResponse
    CustomerMapper ..> CustomerInfo
    CustomerRepository ..> Customer

    CustomerService ..> NotFoundException : throws
    CustomerService ..> ConflictException : throws (email already used)
    Customer ..> InvalidInputException : throws
```

### cart

Shopping carts. **Uses `catalog`** through `CatalogApi`. `CartItem` only stores a `productId`.
Names, prices and stock are looked up live, with one `findProducts` call per cart.

```mermaid
classDiagram
    direction TB

    namespace cart {
        class CartController {
            <<RestController>>
            ~create() CartResponse
            ~findById(Long id) CartResponse
            ~addItem(Long id, AddCartItemRequest) CartResponse
            ~changeQuantity(Long id, Long productId, ChangeQuantityRequest) CartResponse
            ~removeItem(Long id, Long productId) CartResponse
        }
        class CartApi {
            <<public interface>>
            +getCart(Long cartId) CartInfo
            +clearCart(Long cartId)
        }
        class CartService {
            <<Service>>
            ~create() CartResponse
            ~findById(Long id) CartResponse
            ~addItem(Long cartId, AddCartItemRequest) CartResponse
            ~changeQuantity(Long cartId, Long productId, ChangeQuantityRequest) CartResponse
            ~removeItem(Long cartId, Long productId) CartResponse
            -toResponse(Cart) CartResponse
        }
        class CartInfo {
            <<public record>>
            +Long id
            +List~CartLineInfo~ lines
            +isEmpty() boolean
        }
        class CartLineInfo {
            <<public record>>
            +Long productId
            +int quantity
        }
        class CartRepository {
            <<JpaRepository>>
        }
        class Cart {
            <<Entity>>
            -Long id
            -Instant createdAt
            -List~CartItem~ items
            ~create(Instant createdAt)$ Cart
            ~addItem(Long productId, int quantity)
            ~changeQuantity(Long productId, int quantity)
            ~removeItem(Long productId)
            ~clear()
            ~quantityOf(Long productId) int
        }
        class CartItem {
            <<Entity>>
            -Long id
            -Long productId
            -int quantity
        }
        class AddCartItemRequest {
            <<record>>
        }
        class ChangeQuantityRequest {
            <<record>>
        }
        class CartResponse {
            <<record>>
        }
        class CartLineResponse {
            <<record>>
        }
    }

    namespace catalog {
        class CatalogApi {
            <<public interface>>
            +getProduct(Long productId) ProductInfo
            +findProducts(Collection~Long~ productIds) Map
        }
        class ProductInfo {
            <<public record>>
        }
        class InsufficientStockException {
            <<public>>
        }
    }

    CartController --> CartService
    CartController ..> AddCartItemRequest
    CartController ..> ChangeQuantityRequest
    CartApi <|.. CartService
    CartApi ..> CartInfo : returns
    CartInfo *-- CartLineInfo
    CartService --> CartRepository
    CartService ..> CartResponse : builds
    CartResponse *-- CartLineResponse
    CartRepository ..> Cart
    Cart "1" *-- "*" CartItem : @OneToMany

    CartService --> CatalogApi : other feature
    CartService ..> ProductInfo
    CartService ..> InsufficientStockException : throws

    note for CartItem "productId refers to a catalog product<br>by id only - no @ManyToOne Product"
```

### order

Checkout and orders. **Uses `customer`, `cart` and `catalog`**, each through its `XxxApi`, and
publishes `OrderPlacedEvent` for features that don't exist yet. The order keeps *copies* of the
product names, prices and shipping address, so later changes elsewhere don't rewrite history.

```mermaid
classDiagram
    direction TB

    namespace order {
        class OrderController {
            <<RestController>>
            ~placeOrder(PlaceOrderRequest) OrderResponse
            ~findByCustomer(Long customerId) List~OrderSummaryResponse~
            ~findById(Long id) OrderResponse
            ~cancel(Long id) OrderResponse
            ~ship(Long id) OrderResponse
        }
        class OrderService {
            <<Service>>
            ~placeOrder(PlaceOrderRequest) OrderResponse
            ~findByCustomer(Long customerId) List~OrderSummaryResponse~
            ~findById(Long id) OrderResponse
            ~cancel(Long id) OrderResponse
            ~ship(Long id) OrderResponse
        }
        class OrderPlacedEvent {
            <<public record>>
            +Long orderId
            +Long customerId
            +BigDecimal total
            +Instant placedAt
        }
        class OrderRepository {
            <<JpaRepository>>
            ~findByCustomerIdOrderByPlacedAtDesc(Long customerId) List~Order~
        }
        class Order {
            <<Entity>>
            -Long id
            -Long customerId
            -ShippingAddress shippingAddress
            -OrderStatus status
            -Instant placedAt
            -BigDecimal total
            -List~OrderLine~ lines
            ~place(...)$ Order
            ~cancel()
            ~ship()
            ~itemCount() int
        }
        class OrderLine {
            <<Entity>>
            -Long id
            -Long productId
            -String productName
            -BigDecimal unitPrice
            -int quantity
            ~lineTotal() BigDecimal
        }
        class ShippingAddress {
            <<Embeddable>>
            -String recipientName
            -String street
            -String postalCode
            -String city
        }
        class OrderStatus {
            <<enum>>
            PLACED
            SHIPPED
            CANCELLED
        }
        class OrderMapper {
            ~toResponse(Order)$ OrderResponse
            ~toSummary(Order)$ OrderSummaryResponse
        }
        class PlaceOrderRequest {
            <<record>>
        }
        class OrderResponse {
            <<record>>
        }
        class OrderSummaryResponse {
            <<record>>
        }
        class OrderLineResponse {
            <<record>>
        }
        class ShippingAddressResponse {
            <<record>>
        }
    }

    namespace customer {
        class CustomerApi {
            <<public interface>>
        }
        class CustomerInfo {
            <<public record>>
        }
    }

    namespace cart {
        class CartApi {
            <<public interface>>
        }
        class CartInfo {
            <<public record>>
        }
        class CartLineInfo {
            <<public record>>
        }
    }

    namespace catalog {
        class CatalogApi {
            <<public interface>>
        }
        class ProductInfo {
            <<public record>>
        }
    }

    OrderController --> OrderService
    OrderController ..> PlaceOrderRequest
    OrderService --> OrderRepository
    OrderService ..> OrderMapper
    OrderService ..> OrderPlacedEvent : publishes
    OrderMapper ..> OrderResponse
    OrderMapper ..> OrderSummaryResponse
    OrderResponse *-- OrderLineResponse
    OrderResponse *-- ShippingAddressResponse
    OrderRepository ..> Order
    Order "1" *-- "1..*" OrderLine : @OneToMany
    Order *-- ShippingAddress : @Embedded
    Order --> OrderStatus

    OrderService --> CustomerApi : getCustomer
    OrderService --> CartApi : getCart, clearCart
    OrderService --> CatalogApi : findProducts, reserveStock, releaseStock
    OrderService ..> CustomerInfo
    OrderService ..> CartInfo
    OrderService ..> CartLineInfo
    OrderService ..> ProductInfo

    note for Order "customerId: by id only - no @ManyToOne Customer<br>shippingAddress: a copy of the customer's address"
    note for OrderLine "productId: by id only<br>productName, unitPrice: copies, taken at checkout"
```

#### Order status

```mermaid
stateDiagram-v2
    [*] --> PLACED : placeOrder() - stock reserved, cart emptied
    PLACED --> SHIPPED : ship()
    PLACED --> CANCELLED : cancel() - stock released
    SHIPPED --> [*]
    CANCELLED --> [*]
```

Any other transition throws `ConflictException`, which becomes a 409, e.g. cancelling a shipped order.

---

## 4. Database: foreign keys inside a feature, ids across

Hibernate creates the tables from the entities (`ddl-auto=update`).

- **Solid lines** are JPA associations inside one feature. They become real foreign keys.
- **Dashed lines** are id references to another feature. They are plain `BIGINT` columns with no
  foreign key, so the features stay independent and could even get their own databases later.

```mermaid
erDiagram
    categories ||--o{ products : "category_id - FK (catalog)"
    carts ||--o{ cart_items : "cart_id - FK (cart)"
    orders ||--|{ order_lines : "order_id - FK (order)"

    products ||..o{ cart_items : "product_id - id only"
    products ||..o{ order_lines : "product_id - id only"
    customers ||..o{ orders : "customer_id - id only"

    categories {
        bigint id PK
        varchar name UK
    }
    products {
        bigint id PK
        varchar name
        varchar description
        bigint category_id FK
        varchar origin "null for equipment"
        varchar roast_level "LIGHT, MEDIUM, DARK or null"
        decimal price
        int stock
        bigint version "optimistic locking"
    }
    customers {
        bigint id PK
        varchar name
        varchar email UK
        varchar street
        varchar postal_code
        varchar city
    }
    carts {
        bigint id PK
        datetime created_at
    }
    cart_items {
        bigint id PK
        bigint cart_id FK
        bigint product_id "catalog, id only"
        int quantity
    }
    orders {
        bigint id PK
        bigint customer_id "customer, id only"
        varchar ship_to_name "copy"
        varchar ship_to_street "copy"
        varchar ship_to_postal_code "copy"
        varchar ship_to_city "copy"
        varchar status "PLACED, SHIPPED, CANCELLED"
        datetime placed_at
        decimal total
    }
    order_lines {
        bigint id PK
        bigint order_id FK
        bigint product_id "catalog, id only"
        varchar product_name "copy"
        decimal unit_price "copy"
        int quantity
    }
```

---

## 5. Checkout, step by step

What happens when the customer clicks **Place order**. The order feature talks to three other
features, always through their `XxxApi`. Everything inside `placeOrder` is one transaction.

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant SPA as CheckoutPage.js
    participant CC as CustomerController
    participant OC as OrderController
    participant OS as OrderService
    participant CuA as CustomerApi
    participant CaA as CartApi
    participant CatA as CatalogApi
    participant OR as OrderRepository
    participant EV as ApplicationEventPublisher

    C->>SPA: fills in delivery details, clicks Place order
    alt first order from this browser
        SPA->>CC: POST /api/customers
        CC-->>SPA: 201 customer (id saved in localStorage)
    else known customer
        SPA->>CC: PUT /api/customers/{id}
        CC-->>SPA: 200 customer
    end

    SPA->>OC: POST /api/orders {customerId, cartId}
    OC->>OS: placeOrder(request)
    Note over OS,EV: one @Transactional - any exception rolls back every step

    OS->>CuA: getCustomer(customerId)
    CuA-->>OS: CustomerInfo (or NotFoundException - 404)
    OS->>CaA: getCart(cartId)
    CaA-->>OS: CartInfo with productIds and quantities
    opt cart is empty
        OS-->>OC: ConflictException - 409
    end
    OS->>CatA: findProducts(productIds)
    CatA-->>OS: ProductInfo per id (names and prices)

    loop each cart line
        OS->>CatA: reserveStock(productId, quantity)
        Note right of CatA: too few left - InsufficientStockException - 409
    end

    OS->>OR: save(Order.place(...)) with copied names, prices and address
    OS->>CaA: clearCart(cartId)
    OS->>EV: publishEvent(OrderPlacedEvent)
    OS-->>OC: OrderResponse
    OC-->>SPA: 201 Created
    SPA->>SPA: refreshCart (badge shows 0), show confirmation
```

---

## 6. Frontend modules

The frontend is grouped by type, with the same structure as the other course demos: `app.js` lists
the routes, `router.js` renders one page at a time, and shared state (the cart) lives in its own module.
Pages call the API and draw components. Components never call the API themselves.

```mermaid
flowchart TB
    html["index.html<br/>import map: navigo"] --> app["app.js<br/>init(), route list"]
    app --> router["router.js<br/>render(Page, props), data-navigo links"]
    router --> navigo["vendor/navigo.js"]
    app --> pages
    app --> badge

    subgraph pages["pages/"]
        productsPage["ProductsPage"]
        productPage["ProductPage"]
        cartPage["CartPage"]
        checkoutPage["CheckoutPage"]
        ordersPage["OrdersPage"]
        orderPage["OrderPage"]
        profilePage["ProfilePage"]
        errorPages["ErrorPages<br/>NotFound, ServerDown"]
    end

    subgraph components["components/"]
        badge["CartBadge"]
        others["ProductCard, ProductList, CategoryFilter, SearchInput,<br/>QuantityInput, CartLine, CartSummary, CustomerForm,<br/>OrderRow, OrderLineTable, StatusBadge, StockStatus,<br/>ProductImage, Button, ErrorMessage, Notice"]
    end

    subgraph services["services/"]
        catalogJs["catalog.js"]
        cartsJs["carts.js"]
        customersJs["customers.js"]
        ordersJs["orders.js"]
        http["http.js<br/>getJSON, sendJSON, ApiError"]
    end

    pages -->|"draw"| others
    pages -->|"call"| services
    pages --> cart["cart.js<br/>the cart + subscribeToCart()"]
    pages --> session["session.js<br/>cartId + customerId in localStorage"]
    cart --> cartsJs
    cart --> session
    session --> customersJs
    others --> format["format.js<br/>prices, dates"]
    catalogJs & cartsJs & customersJs & ordersJs --> http
    http -->|"fetch /api/..."| caddy[("Caddy → Spring Boot")]
    cart -.->|"subscribeToCart"| badge
```

| Route | Page | Components | Calls (`services/`, or `cart.js` for the cart) |
|-------|------|------------|-----------|
| `/` | `ProductsPage.js` | CategoryFilter, SearchInput, ProductList → ProductCard (ProductImage, StockStatus, Button), ErrorMessage | `getCategories`, `getProducts`, `addToCart` |
| `/products/:id` | `ProductPage.js` | ProductImage, StockStatus, QuantityInput, Button, Notice, ErrorMessage | `getProduct`, `addToCart` |
| `/cart` | `CartPage.js` | CartLine (QuantityInput, Button), CartSummary, ErrorMessage | `refreshCart`, `changeQuantity`, `removeFromCart` |
| `/checkout` | `CheckoutPage.js` | CustomerForm, CartSummary | `refreshCart`, `createCustomer` / `updateCustomer`, `placeOrder` |
| `/orders` | `OrdersPage.js` | OrderRow (StatusBadge) | `getOrders` |
| `/orders/:id` | `OrderPage.js` | OrderLineTable, StatusBadge, Button, ErrorMessage | `getOrder`, `cancelOrder` |
| `/profile` | `ProfilePage.js` | CustomerForm, Button, Notice, ErrorMessage | `getCustomer`, `findCustomerByEmail`, `createCustomer`, `updateCustomer` |
| anything else | `NotFoundPage` in `ErrorPages.js` | | |
| (API down at startup) | `ServerDownPage` in `ErrorPages.js` | | `refreshCart` failed in `app.js` |

---

## Not built yet

Ideas for the next features: login with Spring Security, reviews, wishlist, discount codes,
loyalty points (via `OrderPlacedEvent`), product management and AI recommendations. The README
explains [how to add a feature](README.md#adding-a-feature).
