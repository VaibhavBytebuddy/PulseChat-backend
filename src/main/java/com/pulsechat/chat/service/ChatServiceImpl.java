package com.pulsechat.chat.service;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.RoomResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.dto.TypingIndicatorEvent;
import com.pulsechat.chat.mapper.ChatMapper;
import com.pulsechat.chat.model.ChatRoomEntity;
import com.pulsechat.chat.model.MessageEntity;
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
import java.util.ArrayList;
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
    private final ChatMapper chatMapper;

    public ChatServiceImpl(
        ChatRoomRepository roomRepository,
        MessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate,
        PostHogTelemetryService telemetryService,
        ChatMapper chatMapper
    ) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.telemetryService = telemetryService;
        this.chatMapper = chatMapper;
    }

    @Override
    public RoomResponse createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Room with name '" + request.name() + "' already exists");
        }

        ChatRoomEntity room = chatMapper.toChatRoomEntity(request);
        if (room.getCreatedBy() == null || room.getCreatedBy().isBlank()) {
            room.setCreatedBy("system");
        }
        ChatRoomEntity saved = roomRepository.save(room);

        telemetryService.capture(
            saved.getCreatedBy(),
            "room_created",
            Map.of(
                "room_id", saved.getId(),
                "room_name", saved.getName(),
                "$groups", Map.of("room", saved.getId())
            )
        );

        return chatMapper.toRoomResponse(saved);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        return chatMapper.toRoomResponseList(roomRepository.findAll());
    }

    @Override
    public RoomResponse getRoomById(String roomId) {
        ChatRoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        return chatMapper.toRoomResponse(room);
    }

    @Override
    public MessageResponse sendMessage(String roomId, SendMessageRequest request) {
        long startTime = System.currentTimeMillis();

        ChatRoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        MessageEntity message = chatMapper.toMessageEntity(request, roomId);
        MessageEntity savedMessage = messageRepository.save(message);

        room.setMessageCount(room.getMessageCount() + 1);
        roomRepository.save(room);

        MessageResponse response = chatMapper.toMessageResponse(savedMessage);

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
        List<MessageEntity> messages;

        if (cursor == null) {
            messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        } else {
            messages = messageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursor, pageable);
        }

        List<MessageResponse> responses = chatMapper.toMessageResponseList(messages);

        List<MessageResponse> chronologicalList = new ArrayList<>(responses);
        Collections.reverse(chronologicalList);
        return Collections.unmodifiableList(chronologicalList);
    }

    @Override
    public void broadcastTyping(TypingIndicatorEvent event) {
        messagingTemplate.convertAndSend("/topic/room/" + event.roomId() + "/typing", event);
    }
}
