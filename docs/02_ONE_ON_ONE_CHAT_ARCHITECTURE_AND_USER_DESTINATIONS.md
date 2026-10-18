# 1-on-1 Direct Messaging & User Destination Routing Guide

> **Purpose**: A comprehensive architectural guide explaining user identity resolution, STOMP private user queues (`/user/queue/...`), and the Deterministic Conversation ID pattern using the **WHAT** and **WHY** framework.

---

## 1. User Identity & Connection Binding

### WHAT is User Identity in WebSockets?
* Unlike HTTP where every single request carries an `Authorization` header, a WebSocket verifies authentication **once during the STOMP `CONNECT` handshake**.
* The server decodes the JWT token, resolves the user identity (e.g. `usr_bob_42`), and creates a Java `Principal`.
* The OS TCP socket (`fd`) is mapped to the STOMP session (`sessionId`), which is mapped to the `Principal`:
  $$\text{TCP Socket (fd = 54)} \longleftrightarrow \text{STOMP Session ("sess\_abc")} \longleftrightarrow \text{User ID ("usr\_bob\_42")}$$

### WHY is this necessary?
* Because TCP sockets stay open persistently, binding the socket to an identity allows the server to look up any connected human being and push messages to their specific screen in under 2 milliseconds.

---

## 2. STOMP User Destinations (`/user/queue/...`)

### WHAT is a User Destination?
* In Spring Boot, private messaging uses the `/user` prefix.
* Bob subscribes to: `/user/queue/messages`.
* Alice sends a private message by having the server execute:
  ```java
  messagingTemplate.convertAndSendToUser("usr_bob_42", "/queue/messages", payload);
  ```

### HOW does it work internally?
1. Spring Boot's `DefaultUserDestinationResolver` intercepts the subscription.
2. It rewrites the destination into a session-unique queue:
   $$\text{Rewritten Destination: } \mathbf{/queue/messages-sess\_abc}$$
3. When Alice sends to `"usr_bob_42"`, Spring Boot looks up all active sessions for Bob and writes the frame exclusively into those socket file descriptors.
4. No other user on the server can see or intercept the frame.

---

## 3. The Deterministic Conversation ID Pattern

### WHAT is it?
* Rather than maintaining separate database tables and logic for "rooms" vs "direct messages", top messaging platforms (WhatsApp, Slack, Discord) model 1-on-1 chats as **two-person conversations**.
* The conversation ID is calculated by sorting the two user IDs alphabetically:
  $$\text{conversationId} = \text{"dm\_"} + \min(\text{userId}_1, \text{userId}_2) + \text{"\_"} + \max(\text{userId}_1, \text{userId}_2)$$

### WHY is this an industry standard?
1. **Zero Database Lookups**: The frontend or backend can compute the conversation ID purely in memory without querying MongoDB.
2. **Prevents Duplication**: Whether Alice messages Bob or Bob messages Alice, both resolve to the exact same thread.
3. **Reuses Indexing & Code**: The exact same compound indexes (`roomId + createdAt DESC`), cursor pagination, and PostHog metrics work identically for group chats and private DMs.

---

## 4. Strongly-Typed `MessageType` Enum

### WHAT is it?
* Replaced raw string types (`"TEXT"`) with `MessageType`:
  * `TEXT`: UTF-8 textual content
  * `IMAGE`: Picture uploads (PNG, JPEG, WebP)
  * `VIDEO`: Video recordings (MP4, WebM)
  * `AUDIO`: Voice notes
  * `FILE`: Documents (PDF, ZIP)
  * `SYSTEM`: Join/leave announcements
  * `CODE_SNIPPET`: Syntax-highlighted code blocks

### WHY is it an industry standard?
* Compile-time safety: Prevents typos (`"txt"` vs `"TEXT"`).
* Schema validation: Guarantees that only supported media formats can enter the database.
