# Adding a feature

Copy an existing feature. For example, product reviews:

1. A package `review` with an entity that stores `productId` and `customerId` (ids, not entities)
   and a `static create(...)` that checks its rules.
2. A repository, and a `@Service @Transactional` that checks the ids through `CatalogApi` and
   `CustomerApi` (they throw `NotFoundException` for unknown ids).
3. A controller with request and response records, using Bean Validation (`@Min(1) @Max(5)`).
4. A `ReviewApi` + `ReviewInfo` only if another feature needs reviews.
5. Tests. `./mvnw test` also checks the new package against the [rules](backend.md#packaged-by-feature).
6. Frontend: `services/reviews.js`, components that report through callbacks, and a page (a route
   in `app.js` and a link in `index.html`) or a section on an existing page.

Hibernate (`ddl-auto=update`) creates the new table on the next start.
