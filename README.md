# Coffee Shop

A small web shop for coffee beans and brewing gear: a Spring Boot REST API, a plain JavaScript
frontend and MySQL, all behind Caddy on one address.

> **AI project:** start with [AI-PROJECT.md](AI-PROJECT.md).

```
coffeeshop-api/   Spring Boot REST API (Java 25, Spring Boot 4.1, Spring Data JPA, MySQL)
frontend/         single-page app in plain JavaScript, served by Nginx
Caddyfile         /api/* goes to Spring Boot, everything else to Nginx
compose.*.yaml    dev: MySQL, Nginx and Caddy. full: Spring Boot too
docs/             how it works
```

## Run it

You need JDK 25, an IDE (e.g. IntelliJ IDEA) and Docker Desktop.

```bash
cp .env.example .env                        # once, then set the two passwords
docker compose -f compose.dev.yaml up -d    # MySQL, Nginx and Caddy
```

Start `CoffeeShopApplication` in your IDE and open **http://localhost:8088**.

| To | Run |
|----|-----|
| Run everything in Docker | `docker compose -f compose.full.yaml up -d --build` |
| Stop | `docker compose -f compose.dev.yaml down` (add `-v` to delete the database too) |
| Run the tests | `cd coffeeshop-api && ./mvnw test` |

## Docs

- [Architecture](docs/ARCHITECTURE.md): diagrams of the parts, packages, database and checkout
- [Backend](docs/backend.md): the feature packages and their rules, errors, REST API, tests
- [Frontend](docs/frontend.md): the files and their conventions
- [Adding a feature](docs/adding-a-feature.md)
- [Troubleshooting](docs/troubleshooting.md)
