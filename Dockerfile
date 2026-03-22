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

# Stage 2: Install Node.js MCP tools from the official Node.js image
FROM node:22-slim AS node-build
RUN npm install -g tavily-mcp@0.2.18

# Stage 3: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy Node.js binary and the globally installed tavily-mcp package
COPY --from=node-build /usr/local/bin/node /usr/local/bin/node
COPY --from=node-build /usr/local/lib/node_modules/tavily-mcp /usr/local/lib/node_modules/tavily-mcp
COPY --from=node-build /usr/local/bin/tavily-mcp /usr/local/bin/tavily-mcp

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
