# System Pulse

## Description
System Pulse is a real-time server health monitoring application. Uses an **Agent-based** system.
It acts as a central hub that receives telemetry data (Heartbeats) from remote servers, processes the status (Online/Offline), and pushes real-time updates to a dashboard via WebSockets.

## 🔗 Project Ecosystem
This repository contains the **Backend API**. To see the full project in action, check out the other components:

* **Frontend Dashboard:** [ServersHealth-Next](https://github.com/CaioEd/ServersHealth-Next) (Next.js)
* **Monitoring Agent:** [Go-MetricsAgent](https://github.com/CaioEd/Go-MetricsAgent) (Go/Golang)

## 🚀 Key Features

### Heartbeat Mechanism (Active Monitoring)
Instead of the API reaching out to servers (which fails behind NAT/Firewalls), the servers execute an **Agent** that sends a "Heartbeat" via HTTP POST to this API.
* **Telemetry:** The agent sends CPU, Memory and Disk usage data.
* **Authentication:** Each server is identified by a unique `UUID Token`.
* **Failure Detection:** A scheduled background task checks for "stale" heartbeats. If a server hasn't sent data in the last 45 seconds, it is automatically marked as **OFFLINE**.

### Real-Time Updates
* **WebSockets (STOMP):** Any change in status or new telemetry data is immediately pushed to the connected clients without page refreshes.

## 🛠 Tech Stack
- **Java 21**
- **Spring Boot 3.2.2** (Web, Data JPA, WebSocket)
- **PostgreSQL 16**
- **Docker 25.2.1** & **Docker Compose**
- **Maven 4.0.0**

## 🏗 Architecture
The system follows a distributed architecture:

1.  **Agent (Go):** Collects OS metrics and sends POST requests (`/api/v1/servers/heartbeat`).
2.  **API (Spring Boot):** * **Controller:** Receives the heartbeat.
    * **Service:** Updates the `lastHeartbeat` timestamp and Server Status in the Database.
    * **Scheduler:** Runs periodically to detect silence (Offline servers).
    * **Broker:** Broadcasts updates to the topic `/topic/server-status`.
3.  **Database (PostgreSQL):** Persists server registry and status.
4.  **Frontend (Next.js):** Subscribes to the WebSocket topic to update UI components.

## 📦 How to run

### Prerequisites
* Java 21 or Docker installed.

### 1. Clone the repository
```bash
git clone https://github.com/CaioEd/system-pulse.git
cd system-pulse
```

### 2. Give permissions to the mvnw file
```bash
chmod +x mvnw
```

### 3. Run Spring Boot Application
```bash
./mvnw spring-boot:run
```

## Documentation
The projetc uses springdoc-openapi to generate the API documentation. You can access it at `http://localhost:8080/swagger-ui.html`
In the documentation you will have access to all endpoints and their respective parameters.

## Postman
The project also has a Postman collection with all the endpoints and their respective parameters. You can find it at `postman/servers_spring.postman_collection.json`
You can import it into Postman and use it to test the API.
