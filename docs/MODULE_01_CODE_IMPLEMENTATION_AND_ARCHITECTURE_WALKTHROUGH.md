# Module 1: Code Implementation & Architecture Walkthrough

> **Purpose**: A comprehensive guide explaining every class, method, and configuration implemented in Module 1, mapping directly to our foundational theory of WebSockets, Spring Boot 3, and PostHog telemetry.

---

## 1. Class-to-Concept Architecture Map

```
  HTTP Request / WebSocket Frame
              │
              ▼
  ┌───────────────────────────────────────────────┐
  │ Protocol Layer (config/)                      │
  │ - WebSocketConfig: /ws-chat endpoint + STOMP  │
  │ - CorsConfig: Allow Angular client (Port 4200)│
  └───────────────────────┬───────────────────────┘
                          │
                          ▼
  ┌───────────────────────────────────────────────┐
  │ Controllers (chat/controller/)                │
  │ - RoomRestController: GET/POST /api/rooms     │
  │ - MessageRestController: REST fallback & hist │
  │ - MessageWebSocketController: @MessageMapping │
  └───────────────────────┬───────────────────────┘
                          │
                          ▼
  ┌───────────────────────────────────────────────┐
  │ Service & Telemetry (chat/service/ & core/)   │
  │ - ChatServiceImpl: Business rules + STOMP     │
  │ - PostHogTelemetryService: Virtual Threads    │
  └───────────────────────┬───────────────────────┘
                          │
                          ▼
  ┌───────────────────────────────────────────────┐
  │ Persistence Layer (chat/repository/ & model/) │
  │ - ChatRoomRepository, MessageRepository       │
  │ - MongoDB Collections: "rooms", "messages"    │
  └───────────────────────────────────────────────┘
```

---

## 2. Walkthrough of Key Files

### 2.1 `WebSocketConfig.java` (The STOMP Broker)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}); // 10s Heartbeat
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```
* **WHAT**:
  * Registers the HTTP Upgrade handshake endpoint at `/ws-chat`.
  * Enables `.withSockJS()` as fallback if a restrictive corporate proxy blocks WebSockets.
  * Sets `/app` as the routing prefix for methods with `@MessageMapping`.
  * Sets `/topic` (broadcast) and `/queue` (private) as in-memory broker destinations.
* **WHY**:
  * Prevents writing raw socket handling code; STOMP frames handle routing and serialization automatically.
  * The `10000ms` heartbeat prevents NAT firewalls from dropping idle TCP connections.

---

### 2.2 `PostHogTelemetryService.java` (Asynchronous Non-Blocking Engine)
```java
@Service
public class PostHogTelemetryService {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    ...
    public void capture(String distinctId, String eventName, Map<String, Object> eventProperties) {
        if (!properties.enabled()) return;

        executor.submit(() -> {
            try {
                restClient.post().uri("/capture/").body(payload).retrieve().toBodilessEntity();
            } catch (Exception ex) {
                log.warn("Failed to dispatch PostHog event: {}", ex.getMessage());
            }
        });
    }
}
```
* **WHAT**:
  * Dispatches telemetry using Java 21's `Executors.newVirtualThreadPerTaskExecutor()`.
  * Wraps HTTP calls inside a `try-catch` that isolates exceptions.
* **WHY (The Non-Blocking Isolation Principle)**:
  * Telemetry must **never** block the WebSocket message delivery thread.
  * If PostHog's API is slow or down, the user's chat message is still sent and delivered in under 2ms.

---

### 2.3 `ChatServiceImpl.java` (The Hub of Business Logic)
```java
// 1. Save to MongoDB
MessageDocument savedMessage = messageRepository.save(message);

// 2. Broadcast immediately to all connected room members via STOMP
messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

// 3. Asynchronously record the event in PostHog
telemetryService.capture(
    request.senderId(),
    "message_persisted",
    Map.of(
        "room_id", roomId,
        "content_length", request.content().length(),
        "execution_time_ms", executionTime,
        "$groups", Map.of("room", roomId)
    )
);
```
* **WHAT**:
  * Handles the 3-step sequence: Database write $\rightarrow$ Real-time STOMP broadcast $\rightarrow$ Asynchronous telemetry.
* **WHY**:
  * Combining `SimpMessagingTemplate` and MongoDB gives us both **instantaneous real-time delivery** and **persistent historical retrieval** with cursor-based pagination.

---

### 2.4 Immutable Records for Payloads (`dto/`)
```java
public record SendMessageRequest(
    @NotBlank String senderId,
    @NotBlank String senderName,
    @NotBlank @Size(max = 2000) String content,
    String type,
    Map<String, String> metadata
) {}
```
* **WHAT**: Java 21 immutable record with Jakarta Bean Validation annotations.
* **WHY**: Eliminates mutable state, prevents thread concurrency bugs, and rejects invalid payloads before hitting the database.

---

## 3. Lombok and MapStruct Architecture

### 3.1 Lombok on Domain Documents (`@Getter`, `@Setter`, `@Builder`)
* **WHAT**:
  * Annotated `ChatRoomDocument` and `MessageDocument` with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder`.
* **WHY**:
  * Eliminates hundreds of lines of boilerplate getters, setters, and constructors while preserving clean object mutation for Spring Data MongoDB auditing and persistence.

### 3.2 MapStruct (`ChatMapper.java`)
* **WHAT**:
  * Defined `@Mapper(componentModel = "spring")` interface `ChatMapper`.
  * Generates compile-time implementations for converting between MongoDB documents and Java 21 `record` DTOs.
* **WHY**:
  * **Compile-Time Safety**: Type errors are caught during `mvn compile`, not at runtime.
  * **Zero Reflection Overhead**: Generates direct getter-to-constructor invocations (faster than manual mapping and ModelMapper).
  * **Separation of Concerns**: `ChatServiceImpl` focuses entirely on business logic and STOMP broadcasting without manual conversion boilerplate.
