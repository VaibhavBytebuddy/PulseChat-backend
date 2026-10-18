# Modern Java 21 & Spring Boot 3 Coding Standards & Best Practices Guide

> **Authoritative Engineering Guide** based on official [Spring.io](https://spring.io) reference documentation, modern Java 21 LTS standards, Clean Architecture, enterprise MongoDB patterns, and PostHog server-side telemetry.

---

## 1. Project Organization & Package Conventions

### 1.1 Package by Feature vs. Package by Layer
For maintainable enterprise services, use feature-oriented packages with clear layer distinctions:
```text
com.pulsechat/
├── config/                     # WebSocketConfig, MongoConfig, SecurityConfig, PostHogConfig
├── core/                       # Shared exceptions, security filters, MDC correlation interceptor
│   ├── exception/              # GlobalExceptionHandler, ApiException (RFC 7807)
│   └── telemetry/              # PostHogTelemetryService, TelemetryEvent
├── chat/                       # Feature: Chat & Messaging
│   ├── controller/             # MessageWebSocketController, MessageRestController
│   ├── dto/                    # SendMessageRequest, MessageResponse, ReactionRequest (records)
│   ├── model/                  # MessageDocument, RoomDocument
│   ├── repository/             # MessageRepository, RoomRepository
│   └── service/                # ChatService, ChatServiceImpl
└── user/                       # Feature: User Presence & Profile
```

### 1.2 Class & Variable Naming Standards
* **Controllers**: `*Controller` (`MessageController`, `RoomController`).
* **Services**: Interface `ChatService` + implementation `ChatServiceImpl` (or clean record service).
* **Repositories**: `*Repository` (`MessageRepository`).
* **Documents (Entities)**: `*Document` (`MessageDocument`, `ChatRoomDocument`) to distinguish clearly from DTOs.
* **DTOs & Payloads**: `*Request`, `*Response`, `*Event` (`SendMessageRequest`, `MessageResponse`).
* **Configuration Properties**: `*Properties` (`PostHogProperties`, `WebSocketProperties`).
* **Constants**: `UPPER_SNAKE_CASE` (`DEFAULT_PAGE_SIZE = 50`).

---

## 2. Modern Java 21 Language Standards

### 2.1 Immutable Records for All Data Carriers
* **Always use `record`** for DTOs, API payloads, STOMP messages, and event representations.
* Records provide immutability, thread-safety, standard accessor methods, and canonical constructors with zero boilerplate.

```java
public record SendMessageRequest(
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    String content,

    @NotBlank(message = "Sender ID is required")
    String senderId,

    @NotBlank(message = "Sender display name is required")
    String senderName,

    @Pattern(regexp = "^(TEXT|IMAGE|FILE)$", message = "Type must be TEXT, IMAGE, or FILE")
    String messageType,

    Map<String, String> metadata
) {
    // Compact constructor for validation / normalization
    public SendMessageRequest {
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }
}
```

### 2.2 Sealed Interfaces for Domain Events & Results
* Use `sealed` interfaces to model closed sets of domain events or business outcomes, enabling compiler-checked exhaustive switch statements.

```java
public sealed interface ChatOperationResult permits 
    ChatOperationResult.Success, 
    ChatOperationResult.RoomNotFound, 
    ChatOperationResult.UserMuted {

    record Success(MessageResponse message) implements ChatOperationResult {}
    record RoomNotFound(String roomId) implements ChatOperationResult {}
    record UserMuted(String userId, Instant mutedUntil) implements ChatOperationResult {}
}
```

### 2.3 Pattern Matching & Switch Expressions
* Eliminate verbose `if-else` cascades and explicit casting with pattern-matching switch expressions:

```java
public ResponseEntity<?> handleResult(ChatOperationResult result) {
    return switch (result) {
        case ChatOperationResult.Success s -> ResponseEntity.ok(s.message());
        case ChatOperationResult.RoomNotFound r -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Room not found: " + r.roomId()));
        case ChatOperationResult.UserMuted m -> ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "User is muted until " + m.mutedUntil()));
    };
}
```

### 2.4 Virtual Threads (Project Loom)
* Enable virtual threads in Spring Boot 3 (`application.yml`) to achieve high-throughput, non-blocking I/O with standard synchronous coding style:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

---

## 3. Spring Boot 3 Layered Architecture Standards

### 3.1 Strict Layer Isolation & Entity Encapsulation
* **Rule 1**: Controllers must never directly return MongoDB `@Document` classes. Always convert domain models to immutable response records.
* **Rule 2**: Controllers must never contain business or database query logic.
* **Rule 3**: Repositories must never be injected directly into controllers; always mediate through a service.

### 3.2 Constructor Injection Exclusively
* **Never use field injection (`@Autowired` on private fields)**.
* Use constructor injection with `private final` fields. This guarantees testability and prevents circular dependencies.

```java
@Service
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PostHogTelemetryService telemetry;

    public ChatServiceImpl(
        MessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate,
        PostHogTelemetryService telemetry
    ) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.telemetry = telemetry;
    }
}
```

---

## 4. MongoDB Data Access & Schema Patterns

### 4.1 Schema Design: Never Embed Unbounded Arrays
* **Anti-Pattern**: Embedding a list of all messages inside a `RoomDocument`. Documents will exceed MongoDB's 16MB limit and cause severe write amplification.
* **Best Practice**: Separate collections:
  * `rooms`: Room metadata, member counts, creation timestamp.
  * `messages`: Individual message documents linked by `roomId`.

```java
@Document(collection = "messages")
@CompoundIndexes({
    // Critical index for chat history retrieval
    @CompoundIndex(name = "room_created_desc_idx", def = "{'roomId': 1, 'createdAt': -1}"),
    // Index for user messages lookup
    @CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
})
public class MessageDocument {

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String senderId;
    private String senderName;
    private String content;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version; // Optimistic locking
    
    // Getters and setters
}
```

### 4.2 Cursor-Based Pagination
* Avoid offset-based pagination (`skip()` & `limit()`) for chat history.
* Query by `createdAt < cursorTimestamp` with indexed sort:

```java
public interface MessageRepository extends MongoRepository<MessageDocument, String> {

    List<MessageDocument> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        String roomId,
        Instant cursor,
        Pageable pageable
    );
}
```

### 4.3 Multi-Document Transactions
* When updating multiple collections atomically (e.g., creating a room and initializing default welcome message), use `@Transactional` with Spring's `MongoTransactionManager`:

```java
@Configuration
public class MongoConfig {
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

---

## 5. WebSockets & STOMP Protocol Best Practices

### 5.1 Clean Destination Namespaces
* Application endpoints (client-to-server):
  * `/app/chat.send/{roomId}` - Send message
  * `/app/chat.typing/{roomId}` - Typing indicator
  * `/app/chat.join/{roomId}` - Join room presence
* Broker endpoints (server-to-client broadcasts):
  * `/topic/room/{roomId}` - Public room message feed
  * `/queue/user/{userId}/alerts` - User private notifications

### 5.2 STOMP Channel Interceptors for Security
* Authenticate during STOMP `CONNECT` using a `ChannelInterceptor` rather than inspecting every incoming frame:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}); // 10s heartbeat
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

---

## 6. REST API Standards & RFC 7807 Error Handling

### 6.1 Unified `ProblemDetail` Responses
* Handle all exceptions via `@RestControllerAdvice` returning RFC 7807 `ProblemDetail` objects:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://pulsechat.app/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input data");
        problem.setTitle("Validation Failed");
        
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                (existing, replacement) -> existing
            ));
        problem.setProperty("errors", fieldErrors);
        return problem;
    }
}
```

---

## 7. Configuration Management with `@ConfigurationProperties`

* Always bind properties to validated records or classes; avoid loose `@Value` tags:

```java
@ConfigurationProperties(prefix = "app.posthog")
@Validated
public record PostHogProperties(
    @NotBlank String apiKey,
    @NotBlank String host,
    boolean enabled,
    int batchSize,
    int flushIntervalMs
) {}
```
* Enable via `@ConfigurationPropertiesScan` in the main application class.

---

## 8. PostHog Server-Side Telemetry Standards

### 8.1 Asynchronous & Resilient Dispatch
* Server-side telemetry must **never** block user request threads or WebSocket delivery.
* Use virtual threads or an internal non-blocking executor.
* Always isolate telemetry exceptions so analytics issues never disrupt application operations.

```java
@Service
public class PostHogTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(PostHogTelemetryService.class);
    private final PostHogProperties properties;
    private final RestClient restClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PostHogTelemetryService(PostHogProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.host())
            .build();
    }

    public void capture(String distinctId, String eventName, Map<String, Object> properties) {
        if (!this.properties.enabled()) return;

        executor.submit(() -> {
            try {
                Map<String, Object> enrichedProperties = new HashMap<>(properties);
                enrichedProperties.put("$lib", "posthog-java-spring");
                enrichedProperties.put("environment", "development");

                Map<String, Object> payload = Map.of(
                    "api_key", this.properties.apiKey(),
                    "event", eventName,
                    "distinct_id", distinctId,
                    "properties", enrichedProperties,
                    "timestamp", Instant.now().toString()
                );

                restClient.post()
                    .uri("/capture/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            } catch (Exception ex) {
                log.warn("Failed to capture PostHog server event '{}': {}", eventName, ex.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
```

### 8.2 Event & Property Naming Alignment
* **Consistent Event Names**: Use identical naming conventions across backend and frontend (`[noun] [past_tense_action]`, e.g. `message_persisted`, `room_created`, `stomp_connected`).
* **Property Names**: strictly `snake_case` (`room_id`, `sender_id`, `execution_time_ms`, `message_size_bytes`).
* **Group Analytics**: Add `$groups` properties to correlate events with specific chat rooms:
  ```java
  Map.of("$groups", Map.of("room", roomId))
  ```

---

## 9. Observability, Logging & Clean Code

1. **Structured Logging with MDC (Mapped Diagnostic Context)**:
   * Populate SLF4J MDC with `traceId`, `userId`, and `roomId` in interceptors so all logs from a single user or room action are correlated.
2. **Defensive Programming & Null-Safety**:
   * Always return `Optional<T>` from finders in services/repositories.
   * Never return `null` for collections; return `List.of()`, `Set.of()`, or `Collections.emptyMap()`.
3. **Graceful Shutdown**:
   * Enable `server.shutdown: graceful` in `application.yml` to allow pending WebSocket frames and database writes to complete before process exit.
4. **Environment Variables**:
   * Inject all secrets (`POSTHOG_API_KEY`, `MONGODB_URI`, JWT secrets) via environment variables. Never commit credentials into source control.
