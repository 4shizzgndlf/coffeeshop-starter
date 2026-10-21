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
7. [Features: use cases and user stories](#7-features-use-cases-and-user-stories)

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
    participant SPA as checkoutPage.js
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
    SPA->>SPA: notifyCartChanged (badge shows 0), show confirmation
```

---

## 6. Frontend modules

The frontend is grouped by type, like the course template. Pages call the API and draw components.
Components never call the API themselves.

```mermaid
flowchart TB
    html["index.html<br/>import map: navigo"] --> app["js/app.js<br/>route list"]
    app --> router["js/router.js<br/>page(), data-navigo links"]
    router --> navigo["js/vendor/navigo.js"]
    app --> pages
    app --> badge

    subgraph pages["js/pages/"]
        productsPage["productsPage"]
        productPage["productPage"]
        cartPage["cartPage"]
        checkoutPage["checkoutPage"]
        ordersPage["ordersPage"]
        orderPage["orderPage"]
        profilePage["profilePage"]
        notFoundPage["notFoundPage"]
    end

    subgraph components["js/components/"]
        badge["CartBadge"]
        others["ProductCard, ProductList, CategoryFilter, SearchInput,<br/>QuantityInput, CartLine, CartSummary, CustomerForm,<br/>OrderRow, OrderLineTable, StatusBadge, StockStatus,<br/>ProductImage, Button, ErrorMessage, Notice"]
    end

    subgraph api["js/api/"]
        catalogJs["catalog.js"]
        cartsJs["carts.js"]
        customersJs["customers.js"]
        ordersJs["orders.js"]
        http["http.js<br/>getJSON, sendJSON, ApiError"]
    end

    pages -->|"draw"| others
    pages -->|"call"| api
    pages --> session["js/session.js<br/>cartId + customerId in localStorage"]
    session --> cartsJs & customersJs
    others --> format["js/format.js<br/>prices, dates"]
    catalogJs & cartsJs & customersJs & ordersJs --> http
    http -->|"fetch /api/..."| caddy[("Caddy → Spring Boot")]
    cartsJs -.->|"cart:changed event"| badge
```

| Route | Page | Components | API calls |
|-------|------|------------|-----------|
| `/` | `productsPage.js` | CategoryFilter, SearchInput, ProductList → ProductCard (ProductImage, StockStatus, Button), ErrorMessage | `getCategories`, `getProducts`, `addToCart` |
| `/products/:id` | `productPage.js` | ProductImage, StockStatus, QuantityInput, Button, Notice, ErrorMessage | `getProduct`, `addToCart` |
| `/cart` | `cartPage.js` | CartLine (QuantityInput, Button), CartSummary, ErrorMessage | `getCart`, `changeQuantity`, `removeFromCart` |
| `/checkout` | `checkoutPage.js` | CustomerForm, CartSummary | `createCustomer` / `updateCustomer`, `placeOrder` |
| `/orders` | `ordersPage.js` | OrderRow (StatusBadge) | `getOrders` |
| `/orders/:id` | `orderPage.js` | OrderLineTable, StatusBadge, Button, ErrorMessage | `getOrder`, `cancelOrder` |
| `/profile` | `profilePage.js` | CustomerForm, Button, Notice, ErrorMessage | `getCustomer`, `findCustomerByEmail`, `createCustomer`, `updateCustomer` |
| anything else | `notFoundPage.js` | | |

---

## 7. Features: use cases and user stories

### Use cases

Mermaid has no use case diagram, so this is a flowchart in that style: actors on the left, use cases
(rounded) grouped by feature.

```mermaid
flowchart LR
    customer(["Customer<br/>(browser, no login)"])
    staff(["Staff<br/>(REST API only)"])
    system(["Future feature<br/>(e.g. loyalty)"])

    subgraph catalogUC["catalog"]
        UC1(["Browse products"])
        UC2(["Filter by category"])
        UC3(["Search by name or origin"])
        UC4(["View product details"])
    end

    subgraph cartUC["cart"]
        UC5(["Add product to cart"])
        UC6(["View cart and totals"])
        UC7(["Change quantity"])
        UC8(["Remove product"])
    end

    subgraph customerUC["customer"]
        UC9(["Create profile"])
        UC10(["Find profile by email"])
        UC11(["Edit profile"])
        UC12(["Forget profile in this browser"])
    end

    subgraph orderUC["order"]
        UC13(["Place order"])
        UC14(["View my orders"])
        UC15(["View order details"])
        UC16(["Cancel order"])
        UC17(["Ship order"])
        UC18(["React to a placed order"])
    end

    customer --- UC1 & UC2 & UC3 & UC4
    customer --- UC5 & UC6 & UC7 & UC8
    customer --- UC9 & UC10 & UC11 & UC12
    customer --- UC13 & UC14 & UC15 & UC16
    staff --- UC17
    system -.- UC18

    UC13 -. "includes (first order)" .-> UC9
    UC13 -. "includes" .-> UC6
    UC5 -. "checks stock" .-> UC4
```

### User stories

All of these are implemented, in the REST API and (except US-17 and US-18) in the UI. The *Tests* column
names the tests that cover each story (`CheckoutFlowTest` unless another class is given).

#### Catalog

| ID | User story | Acceptance criteria | Where | Tests |
|----|-----------|---------------------|-------|-------|
| US-01 | As a **customer** I want to **see all products** so that I can choose what to buy. | Each product shows category, name, origin and roast (if coffee), price in DKK and stock status ("In stock", "Only 3 left", "Out of stock"). Sold-out products can't be added. | `/` · `GET /api/products` | `catalogIsFilledWithCategoriesAndProducts` |
| US-02 | As a **customer** I want to **filter by category** so that I only see beans, ground coffee or equipment. | "All" plus one button per category. The chosen one is highlighted. | `/` · `GET /api/categories`, `GET /api/products?categoryId=` | `productsCanBeFilteredByCategory` |
| US-03 | As a **customer** I want to **search by name or origin** so that I find e.g. all Kenyan coffee. | Case-insensitive. Combines with the category filter. Shows a message when nothing matches. | `/` · `GET /api/products?q=` | `productsCanBeSearchedByNameOrOrigin` |
| US-04 | As a **customer** I want to **see a product's details** so that I know what I'm buying. | Description, price and stock. Quantity can be chosen up to the stock (max 99). An unknown product shows "does not exist". | `/products/:id` · `GET /api/products/{id}` | `unknownProductReturns404AsProblemDetail` |

#### Cart

| ID | User story | Acceptance criteria | Where | Tests |
|----|-----------|---------------------|-------|-------|
| US-05 | As a **customer** I want to **add products to a cart** so that I can buy several things at once. | A cart is created on the first add and remembered by the browser. Adding the same product again increases its quantity. More than the stock gives "Only N left of ..." (409). | `/`, `/products/:id` · `POST /api/carts`, `POST /api/carts/{id}/items` | `addingMoreToTheCartThanInStockReturns409`, `CartTest`, `CartServiceTest` |
| US-06 | As a **customer** I want to **see my cart with totals** so that I know what I'll pay. | Lines show today's price, line total and quantity. The summary shows the item count and total. The header badge shows the item count on every page and survives a reload. | `/cart` · `GET /api/carts/{id}` | `fullCheckoutFlow`, `CartServiceTest` |
| US-07 | As a **customer** I want to **change the quantity** of a product in my cart. | 1-99 and at most the stock. A warning appears if the stock dropped below the quantity in the cart. | `/cart` · `PUT /api/carts/{id}/items/{productId}` | `CartTest` |
| US-08 | As a **customer** I want to **remove a product** from my cart. | The line disappears and the totals update. The empty cart links back to the shop. | `/cart` · `DELETE /api/carts/{id}/items/{productId}` | `CartTest` |

#### Customer

| ID | User story | Acceptance criteria | Where | Tests |
|----|-----------|---------------------|-------|-------|
| US-09 | As a **new customer** I want to **enter my name, email and address** so that my order can be delivered. | Done at checkout or on the profile page. Every field is required, and the email must be valid: errors are shown next to the field (400). One profile per email (409). | `/checkout`, `/profile` · `POST /api/customers` | `invalidCustomerReturns400WithAnErrorPerField`, `sameEmailCannotBeUsedTwice` |
| US-10 | As a **returning customer** I want to **find my profile by my email** so that I can use it in a new browser. | Case-insensitive. Shows "No customer has that email" if unknown. *Stand-in for a login.* | `/profile` · `GET /api/customers?email=` | `customerCanBeFoundByEmail` |
| US-11 | As a **customer** I want to **edit my details** so that future orders go to my new address. | Saved details show a confirmation. Existing orders keep the old address. | `/profile`, `/checkout` · `PUT /api/customers/{id}` | `OrderServiceTest` (address copied) |
| US-12 | As a **customer on a shared computer** I want to **forget my profile in this browser**. | The profile is removed from `localStorage`. "My orders" is empty again. The data on the server stays. | `/profile` | - |

#### Orders

| ID | User story | Acceptance criteria | Where | Tests |
|----|-----------|---------------------|-------|-------|
| US-13 | As a **customer** I want to **place an order from my cart** so that I get my coffee. | The stock is reserved and the cart is emptied, and a confirmation with the order number is shown. Names, prices and address are copied into the order. If any product is short on stock, nothing changes (409). An empty cart can't be ordered (409). | `/checkout` · `POST /api/orders` | `fullCheckoutFlow`, `whenStockRunsOutBeforeCheckoutNothingIsChanged`, `emptyCartCannotBeOrdered`, `OrderServiceTest` |
| US-14 | As a **customer** I want to **see my orders** so that I can keep track of them. | Newest first, each with number, date, item count, status and total. | `/orders` · `GET /api/orders?customerId=` | `fullCheckoutFlow` |
| US-15 | As a **customer** I want to **see an order's details**. | Lines with the prices paid, the shipping address and the status. Works as a direct link. | `/orders/:id` · `GET /api/orders/{id}` | `fullCheckoutFlow` |
| US-16 | As a **customer** I want to **cancel an order that hasn't shipped** so that I don't pay for something I no longer want. | Only PLACED orders. The items go back in stock. A second cancel gives 409. | `/orders/:id` · `POST /api/orders/{id}/cancel` | `fullCheckoutFlow`, `OrderTest`, `OrderServiceTest` |
| US-17 | As **staff** I want to **mark an order as shipped**. | Only PLACED orders. A shipped order can no longer be cancelled. *API only. Should require a staff login later.* | `POST /api/orders/{id}/ship` | `shippedOrderCannotBeCancelled`, `OrderTest` |
| US-18 | As a **developer of a new feature** I want to **be told when an order is placed** so that I can react without changing the order code. | `OrderPlacedEvent` (order id, customer id, total, time) is published at checkout. | `OrderService` | `fullCheckoutFlow`, `OrderServiceTest` |

### Not built yet

Ideas for the next features are listed in the README under [Feature ideas](README.md#feature-ideas):
login with Spring Security, reviews, wishlist, discount codes, loyalty points (via US-18),
product management and AI recommendations.
