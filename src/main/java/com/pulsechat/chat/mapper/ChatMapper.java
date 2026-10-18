package com.pulsechat.chat.mapper;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.RoomResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.model.ChatRoomDocument;
import com.pulsechat.chat.model.MessageDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    RoomResponse toRoomResponse(ChatRoomDocument document);

    List<RoomResponse> toRoomResponseList(List<ChatRoomDocument> documents);

    MessageResponse toMessageResponse(MessageDocument document);

    List<MessageResponse> toMessageResponseList(List<MessageDocument> documents);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "messageCount", constant = "0L")
    @Mapping(target = "version", ignore = true)
    ChatRoomDocument toChatRoomDocument(CreateRoomRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roomId", source = "roomId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    MessageDocument toMessageDocument(SendMessageRequest request, String roomId);
}
