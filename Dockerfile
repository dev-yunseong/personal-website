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

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_23.x -o nodesource_setup.sh \
    && bash nodesource_setup.sh \
    && apt install -y nodejs

COPY --from=build /app/build/libs/*.jar app.jar
# GeoLite2 City and ASN are not baked into the image (issue #195): the image is
# public and the GeoLite2 EULA forbids redistributing the databases. Mount a
# host directory holding GeoLite2-City.mmdb and GeoLite2-ASN.mmdb at
# /app/geoip, or point GEOIP_DATABASE_PATH and GEOIP_ASN_DATABASE_PATH at it.
# Without the files geo fields stay null and the datacenter signal stays off.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
