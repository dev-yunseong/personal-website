# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only dependency definition files first to cache the dependency layer
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN --mount=type=secret,id=GITHUB_USERNAME \
    --mount=type=secret,id=GITHUB_TOKEN \
    GITHUB_USERNAME=$(cat /run/secrets/GITHUB_USERNAME) \
    GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) \
    ./gradlew dependencies --no-daemon

# Copy source and build
COPY src/ src/
RUN --mount=type=secret,id=GITHUB_USERNAME \
    --mount=type=secret,id=GITHUB_TOKEN \
    GITHUB_USERNAME=$(cat /run/secrets/GITHUB_USERNAME) \
    GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) \
    ./gradlew clean build -x test --no-daemon

# GeoLite2 City database for request statistics (issue #126).
# Best effort on purpose: without a licence key, without network, or on a
# MaxMind error the build still succeeds and the application runs with
# geo fields null. Placed after the source build so every code change
# invalidates this layer and the database is re-downloaded on each deploy.
RUN --mount=type=secret,id=MAXMIND_LICENCE_KEY sh -c '\
    mkdir -p /geoip && touch /geoip/.keep; \
    if [ ! -s /run/secrets/MAXMIND_LICENCE_KEY ]; then \
        echo "MAXMIND_LICENCE_KEY absent: building without GeoLite2"; exit 0; \
    fi; \
    command -v curl >/dev/null || (apt-get update && apt-get install -y --no-install-recommends curl ca-certificates); \
    curl -fsS "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&suffix=tar.gz&license_key=$(cat /run/secrets/MAXMIND_LICENCE_KEY)" \
        -o /tmp/geolite2.tar.gz \
        && tar -xzf /tmp/geolite2.tar.gz -C /tmp \
        && find /tmp -name GeoLite2-City.mmdb -exec mv {} /geoip/ \; \
        || echo "GeoLite2 download failed: building without it"; \
    rm -f /tmp/geolite2.tar.gz'

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_23.x -o nodesource_setup.sh \
    && bash nodesource_setup.sh \
    && apt install -y nodejs

COPY --from=build /app/build/libs/*.jar app.jar
# Empty when the build had no licence key; app.geoip.database-path then finds
# nothing and geo fields stay null. Override GEOIP_DATABASE_PATH to point at
# a mounted volume instead.
COPY --from=build /geoip/ /app/geoip/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
