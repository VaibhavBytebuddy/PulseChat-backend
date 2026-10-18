package com.pulsechat.chat.service;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.RoomResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.dto.TypingIndicatorEvent;

import java.time.Instant;
import java.util.List;

public interface ChatService {
    RoomResponse createRoom(CreateRoomRequest request);
    List<RoomResponse> getAllRooms();
    RoomResponse getRoomById(String roomId);
    MessageResponse sendMessage(String roomId, SendMessageRequest request);
    List<MessageResponse> getMessages(String roomId, Instant cursor, int limit);
    void broadcastTyping(TypingIndicatorEvent event);
}
