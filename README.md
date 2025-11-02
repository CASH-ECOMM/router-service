# Router Service

REST API gateway service that provides HTTP endpoints and connects to  via gRPC.

## Quick Start

### 1. Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for containerized deployment)
- Downstream services running, configured in `application.yaml`

### 2. Development Scenarios

#### Scenario 1: Router local + all services in Docker
Make sure to have up-to-date repo of each service then run the following for each individual service:
```bash
docker compose up
```
In IntelliJ run configurations add `SPRING_PROFILES_ACTIVE=local` environment variable **OR** run the app with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Router connects to `localhost:50051`, `localhost:50052`, `localhost:50053`, `localhost:50054`

#### Scenario B: Router local + mixed local/Docker services
Run whichever service you want to be in Docker with:
```bash
docker compose up
```
Then in your .env file, change the urls of the service running locally and run 
```bash
source .env.local
```

In IntelliJ run configurations add `SPRING_PROFILES_ACTIVE=local` environment variable **OR** run the app with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Running with Maven

**Build the application:**
```bash
./mvnw clean package -DskipTests
```

**Run the service:**
```bash
./mvnw spring-boot:run
```

Service will start on `http://localhost:8080`

### 3. Running with Docker

**Build and run:**
```bash
docker compose up
```

This builds the application inside the container and starts the service.

**Stop the service:**
```bash
docker compose down
```

## API Documentation

Interactive API documentation is available via Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8080/v3/api-docs
```

## Tech Stack

- **Spring Boot 3.5.6** - REST API framework
- **gRPC Client** - Communication with User Service
- **Protocol Buffers** - Data serialization
- **SpringDoc OpenAPI** - API documentation
- **Maven** - Build tool
- **Docker** - Containerization