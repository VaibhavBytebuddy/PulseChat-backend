package com.pulsechat.chat.controller;

import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.dto.TypingIndicatorEvent;
import com.pulsechat.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class MessageWebSocketController {

    private final ChatService chatService;

    public MessageWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send/{roomId}")
    public void handleMessage(
        @DestinationVariable String roomId,
        @Valid @Payload SendMessageRequest request
    ) {
        chatService.sendMessage(roomId, request);
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void handleTyping(
        @DestinationVariable String roomId,
        @Payload TypingIndicatorEvent event
    ) {
        chatService.broadcastTyping(event);
    }
}
