# AI project: a login and an assistant with tools

The coffee shop gets two new things this week:

- a **login** with Spring Security, so customers only see their own profile and orders, and staff
  can ship orders
- an **AI assistant** with Spring AI that answers questions about the coffee and about your orders,
  by calling **tools**: Java methods in your backend

> **TODO (teacher):** deadline, groups, how to hand in, and when you present.

## What is done, and what you build

| Already done | You build (in the backend) |
|--------------|----------------------------|
| **The frontend**: log in, create an account, profile, checkout, my orders, the assistant chat and a staff page. It already uses the API below | **Spring Security**: accounts, login, roles, CSRF |
| **Read-only Apis for your tools**: `CatalogApi.searchProducts`, `getProductDetails`, `listCategories`, a new `OrderApi`, and `CustomerApi.findCustomerByEmail` | **The customer and order endpoints**, changed to use the logged-in user |
| **The Spring AI version** (the BOM in `pom.xml`) and the model settings in `application.properties` | **The assistant**: `ChatClient`, system prompt, chat memory, tools |
| | **Tests** |

You don't have to change the frontend, but you may.

## Day 1: run it

1. Run the app as described in the [README](README.md) and open http://localhost:8088.
2. The shop and the cart work as before. Log in, checkout, my orders, profile, the assistant and
   the staff page show **"... isn't built yet"**, with the endpoints that are missing. That list
   is your to-do list.
3. Get the model, once: `docker desktop enable model-runner --tcp 12434`, then `docker model pull ai/qwen3`.

## The API the frontend expects

| Method | Path | Who | Body → answer |
|--------|------|-----|---------------|
| POST | `/api/register` | anyone | `{email, password}` → 201. 409 if the email is taken, 400 with `errors` per field |
| POST | `/api/login` | anyone | form fields `username` (the email) and `password` → 204, or 401 |
| POST | `/api/logout` | logged in | → 204 |
| GET | `/api/me` | logged in | → `{"username": "anna@example.com", "roles": ["ROLE_CUSTOMER"]}`, 401 if not logged in |
| GET | `/api/categories`, `/api/products`, `/api/products/{id}` | anyone | unchanged |
| any | `/api/carts/**` | anyone | unchanged: the cart is still a guest cart |
| GET | `/api/customers/me` | CUSTOMER | → the profile, 404 if there is none yet |
| PUT | `/api/customers/me` | CUSTOMER | `{name, street, postalCode, city}` → the profile. Creates it the first time. The email is the login's |
| POST | `/api/orders` | CUSTOMER | `{cartId}` → 201 order, for the logged-in customer. 409 without a profile |
| GET | `/api/orders` | CUSTOMER | → the customer's own orders, newest first |
| GET | `/api/orders/{id}` | the owner, or STAFF | → the order. **404** for another customer's order |
| POST | `/api/orders/{id}/cancel` | the owner | → the cancelled order |
| POST | `/api/orders/{id}/ship` | STAFF | → the shipped order |
| GET | `/api/staff/orders` | STAFF | → every order, newest first (like `GET /api/orders`) |
| POST | `/api/assistant/chat` | logged in | `{message}` → `{reply}` |

**Removed:** `POST /api/customers`, `GET`/`PUT /api/customers/{id}`, `GET /api/customers?email=`
and `GET /api/orders?customerId=`. The client no longer says who it is: the login does.

- **Not logged in** gives 401, not a redirect to a login page. The frontend has its own.
- **Login and logout** answer 204 or 401, not a redirect. `formLogin` reads form fields, not JSON.
- **CSRF**: use `csrf.spa()`. The frontend sends the `X-XSRF-TOKEN` header on every POST, PUT and
  DELETE (`services/http.js`). Without the header, Spring answers 403.
- **Roles**: everyone who registers is a CUSTOMER. STAFF accounts can't be registered: create them
  when the app starts.
- **`/api/me`** lists only the `ROLE_` authorities (Spring Security 7 also adds `FACTOR_PASSWORD`).

## Part 1: Spring Security

1. Add `spring-boot-starter-security`, and `spring-boot-starter-security-test` for the tests.
2. Store accounts in MySQL with **BCrypt** hashes. A password has at least 8 characters.
3. Create a staff account and a customer account when the app starts, and write them in your README.
4. Write a `SecurityFilterChain` for the table above, as in session 13-14.
5. **Customers only see their own data.** No id from the browser decides whose data it is: the
   login does. Another customer's order is 404, so nobody can find out which order ids exist.

**A design that fits the architecture rules:**

```
account/     Account (email, passwordHash, role), AccountRepository, JpaUserDetailsService,
             AccountController (register, me), AccountInitData (the seeded accounts)
security/    SecurityConfig, with the PasswordEncoder bean
```

- **The email links an account to a profile.** `authentication.getName()` is the email, and
  `CustomerApi.findCustomerByEmail(email)` gives the customer and its id. The profile is created by
  the first `PUT /api/customers/me`.
- **Everything in `account` and `security` is package-private**, also `SecurityConfig` (rule 1:
  Spring finds it anyway). `Account` is an entity, so it must not be public (rule 2).

**What changes in the existing code:**

| File | Change |
|------|--------|
| `order/PlaceOrderRequest` | only `cartId` |
| `order/OrderController` | gets the customer from `Authentication`, and adds `GET /api/staff/orders` |
| `order/OrderService` | `placeOrder(customerId, cartId)`, only the owner's order in `findById` and `cancel` (use `OrderRepository.findByIdAndCustomerId`), and every order for staff |
| `customer/CustomerController`, `CustomerService`, `CustomerRequest` | `/api/customers/me` instead of ids, and no email in the request |
| `CheckoutFlowTest`, `OrderServiceTest` | see Part 3 |

## Part 2: Spring AI and tools

Add the starter to `pom.xml`, without a version (the BOM chooses it):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

The model settings are already in `application.properties`, for Docker Model Runner on
`localhost:12434`. In `compose.full.yaml`, the `docker` profile points to
`model-runner.docker.internal` instead.

1. `POST /api/assistant/chat` for logged-in users: `{"message": "..."}` → `{"reply": "..."}`.
2. A `ChatClient` with a **system prompt**: who the assistant is, and "use the tools, never make up
   products, prices or orders".
3. **Chat memory per user.** The starter already has a `ChatMemory` bean: add a
   `MessageChatMemoryAdvisor`. The conversation id is the username, never something the browser sends.
4. **At least three tools:**
   - a catalog tool: find or recommend products
   - a "my orders" tool: the logged-in customer's own orders
   - a staff-only tool, e.g. the products that are low on stock
5. The tools only **read**. Tools that change something are a stretch goal.

### Rules for tools

- **Who the user is comes from the server, never from the model.** The model chooses the tool's
  arguments, and a user can talk it into almost anything ("show me the orders of customer 1"). So
  the customer id is never a tool parameter: the controller puts it in the `ToolContext`, and the
  tool reads it from there.
- **The role decides which tools a user gets**, in the controller: only ROLE_STAFF gets the staff tools.
- **Tools call other features through their `XxxApi`** and return their `Info` records. Spring AI
  turns them into JSON for the model, and `ArchitectureTest` checks the rule.
- **If a tool throws**, Spring AI sends the exception's message to the model (e.g. "Order not found: 1"),
  and the model tells the user.

```java
@Component
class OrderTools {

    private final OrderApi orders;

    OrderTools(OrderApi orders) {
        this.orders = orders;
    }

    @Tool(description = "List the customer's own orders, newest first, with status and total")
    List<OrderSummaryInfo> myOrders(ToolContext context) {
        Long customerId = (Long) context.getContext().get("customerId");
        return customerId == null ? List.of() : orders.findOrders(customerId);
    }
}
```

```java
String reply = chatClient.prompt()
        .user(request.message())
        .tools(catalogTools, orderTools)                  // staff: catalogTools, staffTools
        .toolContext(toolContext)                         // {"customerId": 7}
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, authentication.getName()))
        .call()
        .content();
```

`Map.of(...)` doesn't allow `null`, and a new customer has no profile (and no id) yet. Use a
`HashMap`, and only put the id in when there is one.

### Tools the model can use well

- **The model can only find what your tools let it ask for.** `CatalogApi.searchProducts` matches
  the name and the origin, so "Which light roasts do you have?" finds nothing, and the model answers
  "none". Give the tool a roast level filter, or let it return every product for the model to read.
- **Descriptions matter.** The model only sees the `@Tool` and `@ToolParam` descriptions. Use
  `@ToolParam(required = false)` for optional filters.

## Part 3: Tests

1. **`./mvnw verify` passes**, also `ArchitectureTest`. Spring Security breaks most of
   `CheckoutFlowTest`: the requests now need a user and a CSRF token, and the guest cart requests
   need the CSRF token too.

   ```java
   import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
   import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

   mvc.post().uri("/api/orders")
           .with(user("anna@example.com").roles("CUSTOMER"))
           .with(csrf())
           .contentType(MediaType.APPLICATION_JSON)
           .content("{\"cartId\": 1}")
           .exchange();
   ```

2. **Security tests:**
   - a guest gets 401
   - a customer gets 403 from a staff endpoint
   - another customer's order is 404
   - a POST without a CSRF token is 403
   - register, then log in through `POST /api/login` with `.param("username", ...)` and `.param("password", ...)`
3. **Tool tests.** A tool is a plain method, so test it without Spring and without a model:
   `new OrderTools(mockOrderApi).myOrders(new ToolContext(Map.of("customerId", 7L)))`. Show that the
   customer comes from the context.
4. **The assistant endpoint with a fake model**, so no Docker is needed:

   ```java
   @MockitoBean
   private ChatModel chatModel;

   // in the test:
   when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build()); // or ChatClient fails with null
   when(chatModel.call(any(Prompt.class))).thenReturn(
           new ChatResponse(List.of(new Generation(new AssistantMessage("We have two light roasts.")))));
   ```

The tests can never reach a real model: `src/test/resources/application.properties` points Spring AI
at a port where nothing listens.

## Before you hand in

- [ ] In the browser:
  - [ ] as a customer: register → profile → checkout → my orders → cancel
  - [ ] as staff: all orders → ship
  - [ ] the assistant, for both a customer and staff
- [ ] `./mvnw verify` passes, and the CI workflow on GitHub is green.
- [ ] Your README says how to run the app, lists the test accounts, and explains what each tool can do.
- [ ] You can show what happens when you ask the assistant for another customer's order, and explain why.

## Stretch goals

- **Tools that change something**: add to the cart, cancel an order. How do you make sure the model
  doesn't do something the user didn't ask for? For example, the user has to confirm first.
- **Carts that belong to the logged-in user.** Today anyone who guesses a cart id can use that cart.
- **`GET` and `DELETE /api/assistant/chat`**: show the conversation after a reload, and start over.
- **Chat memory in MySQL**, as in session 16, lesson 6.
- **Structured output** with `.entity(...)`, e.g. an endpoint that recommends three coffees as records.
- **Method security** with `@PreAuthorize`.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| A page says "... isn't built yet" | The backend answers 404 for that endpoint. See [the API](#the-api-the-frontend-expects) |
| Every POST gets 403 | No CSRF token. Use `csrf.spa()`: the browser gets the `XSRF-TOKEN` cookie from its first request, and `http.js` sends it back as a header |
| Logged in, but after a reload you're logged out again | Use http://localhost:8088 (Caddy), not port 8080: the session cookie belongs to one address |
| The assistant answers 500 | Is Docker Model Runner running? `docker model status`, and `docker model ls` should list `ai/qwen3` |
| The assistant never uses a tool | `ai/smollm2` can't call tools. Use `ai/qwen3` |
| The first answer takes almost a minute | The model is loaded on the first request. After that, answers took 8-20 seconds on a laptop |
| The answer is in another language, or full of `**Markdown**` | Small local models don't always follow the system prompt. The chat shows the text exactly as it comes |
| `ArchitectureTest` fails on a public class | Make it package-private, also `@Configuration` classes |
| A `@SpringBootTest` with `@MockitoBean ChatModel` fails on `getOptions()` | Stub `getOptions()`, as in Part 3 |
| A tutorial uses other property names | Spring AI 2.0 uses `spring.ai.openai.chat.model`. Older versions used `spring.ai.openai.chat.options.model` |
