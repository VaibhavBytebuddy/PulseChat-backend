package com.pulsechat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SendMessageRequest(
    @NotBlank(message = "Sender ID is required")
    String senderId,

    @NotBlank(message = "Sender name is required")
    String senderName,

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2000, message = "Content cannot exceed 2000 characters")
    String content,

    String type,

    Map<String, String> metadata
) {
    public SendMessageRequest {
        type = (type == null || type.isBlank()) ? "TEXT" : type;
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }
}
