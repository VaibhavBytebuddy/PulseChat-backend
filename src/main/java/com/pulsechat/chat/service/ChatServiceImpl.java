package com.pulsechat.chat.service;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.RoomResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.dto.TypingIndicatorEvent;
import com.pulsechat.chat.model.ChatRoomDocument;
import com.pulsechat.chat.model.MessageDocument;
import com.pulsechat.chat.repository.ChatRoomRepository;
import com.pulsechat.chat.repository.MessageRepository;
import com.pulsechat.core.exception.ResourceNotFoundException;
import com.pulsechat.core.telemetry.PostHogTelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatRoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PostHogTelemetryService telemetryService;

    public ChatServiceImpl(
        ChatRoomRepository roomRepository,
        MessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate,
        PostHogTelemetryService telemetryService
    ) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.telemetryService = telemetryService;
    }

    @Override
    public RoomResponse createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Room with name '" + request.name() + "' already exists");
        }

        ChatRoomDocument room = new ChatRoomDocument(
            request.name(),
            request.description(),
            request.createdBy() != null ? request.createdBy() : "system"
        );
        ChatRoomDocument saved = roomRepository.save(room);

        telemetryService.capture(
            saved.getCreatedBy(),
            "room_created",
            Map.of(
                "room_id", saved.getId(),
                "room_name", saved.getName(),
                "$groups", Map.of("room", saved.getId())
            )
        );

        return toRoomResponse(saved);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
            .map(this::toRoomResponse)
            .toList();
    }

    @Override
    public RoomResponse getRoomById(String roomId) {
        ChatRoomDocument room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        return toRoomResponse(room);
    }

    @Override
    public MessageResponse sendMessage(String roomId, SendMessageRequest request) {
        long startTime = System.currentTimeMillis();

        ChatRoomDocument room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        MessageDocument message = new MessageDocument(
            roomId,
            request.senderId(),
            request.senderName(),
            request.content(),
            request.type(),
            request.metadata()
        );
        MessageDocument savedMessage = messageRepository.save(message);

        room.setMessageCount(room.getMessageCount() + 1);
        roomRepository.save(room);

        MessageResponse response = toMessageResponse(savedMessage);

        // Broadcast to all active WebSocket listeners on /topic/room/{roomId}
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        long executionTime = System.currentTimeMillis() - startTime;

        // Dispatches telemetry asynchronously via Virtual Threads
        telemetryService.capture(
            request.senderId(),
            "message_persisted",
            Map.of(
                "room_id", roomId,
                "message_id", savedMessage.getId(),
                "content_length", request.content().length(),
                "message_type", request.type(),
                "execution_time_ms", executionTime,
                "$groups", Map.of("room", roomId)
            )
        );

        return response;
    }

    @Override
    public List<MessageResponse> getMessages(String roomId, Instant cursor, int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));
        List<MessageDocument> messages;

        if (cursor == null) {
            messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        } else {
            messages = messageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursor, pageable);
        }

        // Return chronological order for UI consumption
        List<MessageResponse> responses = messages.stream()
            .map(this::toMessageResponse)
            .toList();
        
        List<MessageResponse> mutableList = new java.util.ArrayList<>(responses);
        Collections.reverse(mutableList);
        return Collections.unmodifiableList(mutableList);
    }

    @Override
    public void broadcastTyping(TypingIndicatorEvent event) {
        messagingTemplate.convertAndSend("/topic/room/" + event.roomId() + "/typing", event);
    }

    private RoomResponse toRoomResponse(ChatRoomDocument doc) {
        return new RoomResponse(
            doc.getId(),
            doc.getName(),
            doc.getDescription(),
            doc.getCreatedBy(),
            doc.getCreatedAt(),
            doc.getMessageCount()
        );
    }

    private MessageResponse toMessageResponse(MessageDocument doc) {
        return new MessageResponse(
            doc.getId(),
            doc.getRoomId(),
            doc.getSenderId(),
            doc.getSenderName(),
            doc.getContent(),
            doc.getType(),
            doc.getCreatedAt(),
            doc.getMetadata()
        );
    }
}
