# Stage 1: Build with Gradle and JDK 21
FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle bootJar

# Stage 2: Run with GraalVM CE Java 21
FROM gcr.io/distroless/java21-debian12:latest AS runner

WORKDIR /app

# Define parameters that can be supplied to the image when we call Docker build
ARG APP_PORT=8080
ARG PG_USER=sheikhimtiaz
ARG PG_PASSWORD=" "
ARG PG_DATABASE=vpp
ARG PG_HOST=localhost
ARG PG_PORT=5432

# Define parameters that can be supplied to the image during build or during run
ENV APP_PORT=$APP_PORT
ENV PG_USER=$PG_USER
ENV PG_PASSWORD=$PG_PASSWORD
ENV PG_DATABASE=$PG_DATABASE
ENV PG_HOST=$PG_HOST
ENV PG_PORT=$PG_PORT

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

