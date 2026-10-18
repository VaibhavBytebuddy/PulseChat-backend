# Module 1: Architecture, Configuration & Telemetry Foundation

> **Module Goal**: Establish the core Spring Boot 3 configuration, understand the real-time WebSocket/STOMP messaging architecture, and build an enterprise-grade asynchronous PostHog telemetry engine.

---

## 1. System Design: Why WebSockets & STOMP?

### 1.1 The Limitation of Traditional HTTP
In standard HTTP/1.1 or HTTP/2:
* Communication is **unidirectional and client-driven**: the client sends a request, the server responds.
* The server **cannot push data** to the client spontaneously.
* In a chat application, using HTTP requires:
  1. **Short Polling**: Client requests `GET /messages` every 1–2 seconds.
     * *Problem*: Extreme server load, 95%+ wasted empty requests, latency is bounded by the polling interval.
  2. **Long Polling**: Server holds the HTTP request open until a new message arrives.
     * *Problem*: High overhead of HTTP headers on each reconnect, connection drops, socket exhaustion.

### 1.2 The WebSocket Solution
* **Full-Duplex Bidirectional TCP Connection**: Initiated via an HTTP Upgrade handshake (`GET /ws-chat` with `Upgrade: websocket`).
* Once established, both client and server can transmit lightweight frames with minimal framing overhead (2 to 10 bytes vs. hundreds of bytes of HTTP headers).
* Extremely low latency (sub-millisecond frame delivery).

### 1.3 Why Layer STOMP on Top of Raw WebSockets?
Raw WebSockets are just an open byte/text pipe without any application-level semantics:
* Raw WebSockets do **not** define:
  * How to address a message to a specific room or topic.
  * How to subscribe or unsubscribe.
  * How to format headers or payloads.
* **STOMP (Simple Text Oriented Messaging Protocol)** solves this by defining standard frames:
  * `CONNECT` / `CONNECTED`
  * `SUBSCRIBE` / `UNSUBSCRIBE`
  * `SEND`
  * `MESSAGE`

### 1.4 The Channel Hierarchy in PulseChat
```
[ Angular Client ]
       │
       ├── STOMP SEND ──► /app/chat.send/general ──────► [ Spring MessageController ]
       │                                                         │
       │                                                         ▼ (Persist to Mongo)
       │                                                         ▼ (SimpMessagingTemplate)
       └── STOMP SUBSCRIBE ◄── /topic/room/general ◄─────────────┘ (Broadcast)
```
* **`/app` (Application Destination Prefix)**:
  * Any message sent to `/app/*` routes directly into Spring's `@MessageMapping` controller methods for processing, validation, and database storage.
* **`/topic` (Pub/Sub Broadcast Prefix)**:
  * Fan-out channels. Every client subscribed to `/topic/room/{roomId}` receives a copy of the broadcast.
* **`/queue` (Point-to-Point Prefix)**:
  * Targeted channels for private user notifications (e.g. `/queue/user/{userId}/alerts`).

---

## 2. Modern Java 21 & Virtual Threads (Project Loom)

### 2.1 Platform Threads vs. Virtual Threads
* **Platform Threads (Legacy Java)**:
  * 1:1 mapping to Operating System (OS) threads.
  * Consumes ~1MB of stack memory per thread.
  * Max concurrent threads per JVM: typically 2,000 to 5,000 before Out-Of-Memory (OOM) or CPU context-switch thrashing.
* **Virtual Threads (Java 21 LTS)**:
  * Managed entirely by the JVM, multiplexed over a small pool of OS "carrier threads".
  * Consumes only a few hundred bytes of heap memory per thread.
  * Millions of virtual threads can run concurrently.
  * When a thread performs blocking I/O (waiting on MongoDB or PostHog HTTP requests), the JVM **unmounts** the virtual thread from the carrier thread until the I/O completes.

### 2.2 Enabling in Spring Boot 3
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
This enables Spring MVC and task executors to handle every incoming request and background telemetry task on lightweight virtual threads automatically.

---

## 3. PostHog Server-Side Telemetry Architecture

### 3.1 Why Server-Side Telemetry Matters
Client-side analytics (in Angular) can be blocked by ad-blockers (uBlock Origin, Brave Shields) or lost during network disconnects.
Server-side telemetry provides:
1. **Ground Truth**: Exact records of database writes and confirmed deliveries.
2. **System Latency Metrics**: Time taken to parse, validate, and persist messages.
3. **Cross-Layer Correlation**: Matching server events with the frontend user using the same `distinct_id`.

### 3.2 The Non-Blocking Isolation Principle
* **Golden Rule**: Telemetry must **never** degrade core application performance. If PostHog's API is slow, rate-limited, or experiencing downtime, chat messages must continue to send and deliver without interruption.
* **Implementation Strategy**:
  1. Telemetry calls are delegated to an isolated virtual-thread `ExecutorService`.
  2. All exceptions are caught, logged, and isolated from the user's transaction.
  3. Clean shutdown hook (`@PreDestroy`) flushes any in-flight events before server termination.

### 3.3 Event Schema & Naming Standards
* **Event Naming**: `[noun] [past_tense_action]`
  * `server_started`
  * `room_created`
  * `message_persisted`
  * `stomp_connected`
* **Properties**: Strictly `snake_case`:
  * `room_id`: Channel identifier
  * `sender_id`: User identifier
  * `execution_time_ms`: Processing latency
  * `$lib`: Identified as `posthog-java-spring`
* **Group Analytics**:
  * Associates events with Chat Rooms: `"$groups": { "room": roomId }`.

---

## 4. Verification & Testing Checklist for Module 1
1. Verify Spring Boot compiles cleanly with Java 21 (`mvn clean compile`).
2. Verify WebSocket endpoint `/ws-chat` is registered with SockJS.
3. Verify CORS configuration allows local Angular frontend (`localhost:4200`).
4. Verify PostHog configuration properties are type-safe and validated.
