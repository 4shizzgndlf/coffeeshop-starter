# AI project: a login and an assistant

From: the owner of the coffee shop

The web shop works, but anyone can see anyone's orders, and our customers have many questions.
We want a login, and an assistant that can answer questions.

## What the coffee owner wants

### Customers
- Anyone can browse the shop and fill a cart without logging in.
- Customers create an account with their email and a password.
- Customers log in to check out, and to see their orders and delivery details.
- A customer only ever sees their own details and orders.

### Staff
- Staff log in with accounts we create for them. They can't sign up themselves.
- Staff see every order, and mark orders as shipped.
- Only staff can ship an order.

### The assistant
- Everyone who is logged in can chat with the assistant.
- It answers questions about our products: what we sell, prices, stock, and what to buy.
- It tells customers about their own orders. Never about someone else's.
- It tells staff which products are running low.
- It never makes up products, prices or orders.
- It remembers the conversation.
- It can't change anything: no new or cancelled orders.

### Later
- The assistant could add products to the cart, but only after the customer says yes.

---

## Hints

**The frontend is done.** Run the app as in the [README](README.md). The pages that need the
backend say **"... isn't built yet"**, and list the endpoints they need. Build them in the backend.

### The API the frontend expects

| Method | Path | Who | Body → answer |
|--------|------|-----|---------------|
| POST | `/api/register` | anyone | `{email, password}` → 201, 409 if the email is taken |
| POST | `/api/login` | anyone | form fields `username` (the email) and `password` → 204 or 401 |
| POST | `/api/logout` | logged in | → 204 |
| GET | `/api/me` | logged in | → `{"username": "anna@example.com", "roles": ["ROLE_CUSTOMER"]}`, 401 if not |
| GET, PUT | `/api/customers/me` | CUSTOMER | `{name, street, postalCode, city}`. GET is 404 until the first PUT |
| POST | `/api/orders` | CUSTOMER | `{cartId}` → 201 |
| GET | `/api/orders` | CUSTOMER | the customer's own orders |
| GET | `/api/orders/{id}` | the owner, or STAFF | 404 for someone else's order |
| POST | `/api/orders/{id}/cancel` | the owner | |
| POST | `/api/orders/{id}/ship` | STAFF | |
| GET | `/api/staff/orders` | STAFF | every order |
| POST | `/api/assistant/chat` | logged in | `{message}` → `{reply}` |

Products, categories and `/api/carts/**` stay as they are, open to everyone. The endpoints that use
ids or emails to find a customer (`?email=`, `?customerId=`, `/api/customers/{id}`) go away.

### Spring Security

- **Login as in session 13-14:**
  - `formLogin` on `/api/login`, answering 204 or 401 instead of a redirect
  - a 401 when nobody is logged in
  - `csrf.spa()`: the frontend already sends the `X-XSRF-TOKEN` header
- **Accounts** go in the database, with BCrypt hashes. Two roles: CUSTOMER and STAFF.
- **The email links an account to a customer:** `authentication.getName()`, then
  `CustomerApi.findCustomerByEmail(...)`.
- **ArchitectureTest:** make new classes package-private, also `SecurityConfig`.

### Spring AI

- **Setup:**
  - add `spring-ai-starter-model-openai` to `pom.xml`, without a version
  - the model settings are already in `application.properties`
  - get the model: `docker model pull ai/qwen3` (`ai/smollm2` can't call tools)
- **Memory:** add a `MessageChatMemoryAdvisor`, and use the username as the conversation id.
- **Tools** are `@Tool` methods that call other features' `XxxApi`, such as `CatalogApi`, `OrderApi` and `CustomerApi`.
- **Never let the model choose whose data it reads.** The controller puts the customer id in the
  `ToolContext`, and the tool reads it from there:

  ```java
  @Tool(description = "List the customer's own orders, newest first")
  List<OrderSummaryInfo> myOrders(ToolContext context) {
      return orders.findOrders((Long) context.getContext().get("customerId"));
  }

  // in the controller
  chatClient.prompt()
          .user(message)
          .tools(catalogTools, orderTools)   // staff get other tools
          .toolContext(Map.of("customerId", customerId))   // Map.of throws on null: a new customer has no id yet
          .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, authentication.getName()))
          .call()
          .content();
  ```

- **The model can only find what a tool lets it ask for.** `CatalogApi.searchProducts` searches
  names and origins, so "light roasts" finds nothing unless your tool can filter by roast level.

