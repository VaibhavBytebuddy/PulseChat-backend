package com.pulsechat.chat.controller;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.RoomResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomRestController {

    ChatService chatService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms() {
        return ResponseEntity.ok(chatService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String id) {
        return ResponseEntity.ok(chatService.getRoomById(id));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse created = chatService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
