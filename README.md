# PulseChat-backend

Modern enterprise real-time chat backend built with **Java 21**, **Spring Boot 3**, **MongoDB**, **WebSockets (STOMP)**, and **PostHog** telemetry.

## Features
- **Modern Java 21 LTS**: Immutable records for DTOs/messages, pattern matching, virtual threads enabled.
- **WebSocket & STOMP Protocol**: Scalable bidirectional messaging channels (`/topic/room/{roomId}`, `/app/chat.send/{roomId}`).
- **MongoDB Data Layer**: Separate `rooms` and `messages` collections with compound indexes (`roomId + createdAt DESC`) and cursor-based pagination.
- **PostHog Server-Side Telemetry**: Asynchronous non-blocking analytics dispatching (`message_persisted`, `room_created`, `stomp_connected`).
- **Clean Architecture & RFC 7807**: Unified `ProblemDetail` error responses via `@RestControllerAdvice`.
