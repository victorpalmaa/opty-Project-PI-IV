# Opty Socket Server

> WebSocket service for the Opty project

Real-time chat server for communication between client and supervisor.

## 🚀 Quick Start

### Requirements
- Java 17+
- Maven 3.6+

### Build and Run

#### Using script (Linux/MacOS)

```bash
./scripts/run.sh
```

#### Using Maven

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Server will be available at `http://localhost:8080`

### Run Tests

#### Using script (Linux/MacOS)

```bash
# Run all tests
./scripts/tests.sh

# Run specific test
./scripts/tests.sh SessionTest
./scripts/tests.sh MessageTest
```

#### Using Maven

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SessionTest
mvn test -Dtest=MessageTest

# Run INTRACLASSE tests
mvn test -Dtest=SessionTest,MessageTest
```

**Test Coverage:** 41/44 tests passing (93%)
- ✅ INTRACLASSE: 34/34 tests (100%)
- ✅ INTERCLASSE: 7/10 tests (70%)

### Check Status

```bash
curl http://localhost:8080/actuator/health
```

## 📡 WebSocket Endpoints

### Client
```
ws://localhost:8080/ws/client
```
- Client connects automatically
- Receives `sessionId` from server
- Waits for supervisor to join the session

### Supervisor
```
ws://localhost:8080/ws/supervisor
```
- Connects and receives list of available sessions
- Sends `sessionId` to join client's session
- Exchanges messages in real-time

## 💬 Message Format

```json
{
  "sessionId": "session-uuid",
  "from": "CLIENT" | "SUPERVISOR",
  "type": "CONNECT" | "MESSAGE" | "DISCONNECT" | "ERROR",
  "payload": {},
  "timestamp": "2025-01-01T12:00:00Z"
}
```

## 🔧 Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

opty:
  socket:
    max-connections: 100
    message:
      max-size: 65536
    session:
      timeout-minutes: 30
    cors:
      allowed-origins: "*"
```

## 📂 Project Structure

```
src/main/java/com/opty/socket/
├── config/          # Configuration classes
├── controller/      # REST API controllers
├── dto/             # Data Transfer Objects
├── model/           # Data models
├── service/         # Business logic
└── websocket/       # WebSocket handlers
```

### Main Components

- **SessionManager**: Manages sessions and connections
- **MessageRouter**: Routes messages between client and supervisor
- **SupervisorQueueService**: Distributes session list to supervisors
- **ClientWebSocketHandler**: Client connection handler
- **SupervisorWebSocketHandler**: Supervisor connection handler

## 🔄 Communication Flow

```
1. Client connects → Receives sessionId
2. Supervisor connects → Sees available sessions
3. Supervisor chooses sessionId → Connects with client
4. Messages flow in real-time between both
```

## 🧪 Test with wscat

```bash
# Install
npm install -g wscat

# Client
wscat -c ws://localhost:8080/ws/client

# Supervisor (use sessionId from client)
wscat -c ws://localhost:8080/ws/supervisor
```

## 📋 REST API

### List Available Sessions
```bash
GET http://localhost:8080/api/sessions/available
```

Returns:
```json
[
  {
    "sessionId": "abc-123",
    "paired": false,
    "createdAt": "2025-01-01T12:00:00Z",
    "waitingTimeMinutes": 2,
    "waitingTimeSeconds": 30
  }
]
```

## 🐛 Troubleshooting

### Port in use
```bash
SERVER_PORT=9090 mvn spring-boot:run
```

### CORS blocked
Edit `application.yml`:
```yaml
opty.socket.cors.allowed-origins: "http://localhost:3000,http://localhost:5173"
```

## 📊 Metrics and Monitoring

Available endpoints:
- `/actuator/health` - Server status
- `/actuator/metrics` - System metrics
- `/actuator/info` - Application information

## 📄 License

Copyright © 2025 Opty Development Team
