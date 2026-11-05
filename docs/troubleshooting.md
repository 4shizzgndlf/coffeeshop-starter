# Troubleshooting

| Problem | Fix |
|---------|-----|
| "Cannot reach the API (HTTP 502). Is Spring Boot running?" | Start `CoffeeShopApplication`. In full mode: `docker compose -f compose.full.yaml ps` |
| Spring Boot: `Access denied for user` | `.env` is missing, or its password differs from the one MySQL was created with. Fix `.env`, or start over with `down -v` |
| Linux: Caddy can't reach Spring Boot in your IDE | Check that your firewall lets Docker reach port 8080 (`host.docker.internal` is set in `compose.dev.yaml`) |
| You renamed a field and the old column is still there | `ddl-auto=update` never drops columns: `down -v` and start again |
| GitHub Actions: `./mvnw: Permission denied` | `git update-index --chmod=+x coffeeshop-api/mvnw`, then commit |
