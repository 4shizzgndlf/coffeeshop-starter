# ---------- Build stage: compile and package the jar ----------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

# Copy the Maven wrapper and pom.xml first. Dependencies are then downloaded in
# their own layer, which Docker reuses until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Tests run in CI (./mvnw verify), not here, so building the image stays fast.
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---------- Runtime stage: only a JRE and the jar ----------
FROM eclipse-temurin:25-jre

# curl is only used by the healthcheck in compose.full.yaml
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user, not root
RUN groupadd --system app \
    && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
