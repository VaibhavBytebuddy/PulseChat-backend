package com.pulsechat.chat.controller;

import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageRestController {

    ChatService chatService;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
        @PathVariable String roomId,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(chatService.getMessages(roomId, cursor, limit));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
        @PathVariable String roomId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = chatService.sendMessage(roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
