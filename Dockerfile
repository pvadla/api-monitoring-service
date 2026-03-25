# =============================================================================
#  Stage 1 — Build
#
#  Uses the official Maven + JDK 17 image.
#  The frontend-maven-plugin downloads Node v20 and npm, then runs:
#    npm install  →  npm run build  (Vite → frontend/dist/)
#  maven-resources-plugin copies frontend/dist/ → target/classes/static/
#  spring-boot-maven-plugin packages everything into a single fat JAR.
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# ── Resolve Maven dependencies first ─────────────────────────────────────────
# This layer is cached as long as pom.xml hasn't changed, so repeated builds
# skip the multi-minute download of Spring Boot + all transitive deps.
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# ── Copy source trees ─────────────────────────────────────────────────────────
COPY src        ./src
COPY frontend   ./frontend

# ── Full build ────────────────────────────────────────────────────────────────
# frontend-maven-plugin downloads Node v20.18.0 into target/ (installDirectory),
# runs npm install + npm run build (tsc + vite), then Maven packages the fat JAR.
RUN mvn clean package -DskipTests -B

# =============================================================================
#  Stage 2 — Runtime
#
#  Minimal JRE-only image; non-root user; container-aware JVM tuning.
# =============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# Non-root user for security
RUN addgroup --system spring \
 && adduser  --system --ingroup spring spring

WORKDIR /app

COPY --from=builder /workspace/target/APIMonitoring-0.0.1-SNAPSHOT.jar app.jar

USER spring

# 8080 is the default; Railway injects $PORT at runtime and application-prod.yml
# reads it via  server.port: ${PORT:8080}
EXPOSE 8080

# Shell form so ${PORT} is expanded by sh at container start.
# -XX:+UseContainerSupport  → honour cgroup memory/CPU limits (default in JDK 11+, explicit for clarity)
# -XX:MaxRAMPercentage=75   → use up to 75 % of container RAM for the JVM heap
CMD exec java \
      -XX:+UseContainerSupport \
      -XX:MaxRAMPercentage=75.0 \
      -Dspring.profiles.active=prod \
      -jar /app/app.jar
