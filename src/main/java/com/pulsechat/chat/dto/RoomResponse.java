package com.pulsechat.chat.dto;

import java.time.Instant;

public record RoomResponse(
    String id,
    String name,
    String description,
    String createdBy,
    Instant createdAt,
    long messageCount
) {}
