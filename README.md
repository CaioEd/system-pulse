# System Pulse

## Description
System Pulse is an application that monitors the health of servers.

## Tech Stack
- Java 21
- Spring Boot 3.2.2
- Maven 4.0.0
- PostgreSQL 16
- Docker 25.2.1
- Docker Compose 1.35.2


## How to run
```bash
windows: ./mvnw clean install
```

```bash
linux: mvn clean install
```

```bash
database: docker compose -f docker-compose.db.yml up -d
```

```bash
application: mvn spring-boot:run
```

Disclaimer: 
Before running the application, make sure to have the database running.
All the necessary tables will be created automatically.
The docker files are in the /docker folder.

The application can also be run as a jar file:
```bash
application: java -jar target/system-pulse-0.0.1-SNAPSHOT.jar
```

## Architecture
For the API, we are using the traditional MVC architecture, with models, repositories, services and controllers.
For the database, we are using the traditional JPA architecture, with entities, repositories and services.
