package com.pulsechat.chat.mapper;

import com.pulsechat.chat.dto.CreateRoomRequest;
import com.pulsechat.chat.dto.MessageResponse;
import com.pulsechat.chat.dto.RoomResponse;
import com.pulsechat.chat.dto.SendMessageRequest;
import com.pulsechat.chat.model.ChatRoomEntity;
import com.pulsechat.chat.model.MessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    RoomResponse toRoomResponse(ChatRoomEntity entity);

    List<RoomResponse> toRoomResponseList(List<ChatRoomEntity> entities);

    MessageResponse toMessageResponse(MessageEntity entity);

    List<MessageResponse> toMessageResponseList(List<MessageEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "messageCount", constant = "0L")
    @Mapping(target = "version", ignore = true)
    ChatRoomEntity toChatRoomEntity(CreateRoomRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roomId", source = "roomId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    MessageEntity toMessageEntity(SendMessageRequest request, String roomId);
}
