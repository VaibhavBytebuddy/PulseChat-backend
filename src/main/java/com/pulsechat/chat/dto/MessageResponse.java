package com.pulsechat.chat.dto;

import java.time.Instant;
import java.util.Map;

public record MessageResponse(
    String id,
    String roomId,
    String senderId,
    String senderName,
    String content,
    String type,
    Instant createdAt,
    Map<String, String> metadata
) {}
