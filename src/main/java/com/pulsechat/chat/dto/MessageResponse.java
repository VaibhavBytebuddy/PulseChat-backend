package com.pulsechat.chat.dto;

import com.pulsechat.chat.model.MessageType;
import java.time.Instant;
import java.util.Map;

public record MessageResponse(
    String id,
    String roomId,
    String senderId,
    String senderName,
    String content,
    MessageType type,
    Instant createdAt,
    Map<String, String> metadata
) {}
