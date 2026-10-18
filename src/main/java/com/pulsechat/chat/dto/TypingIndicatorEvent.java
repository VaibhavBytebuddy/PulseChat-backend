package com.pulsechat.chat.dto;

public record TypingIndicatorEvent(
    String roomId,
    String userId,
    String username,
    boolean isTyping
) {}
