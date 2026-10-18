package com.pulsechat.chat.model;

import com.pulsechat.core.entity.MongoBaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "room_created_desc_idx", def = "{'roomId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
})
public class MessageEntity extends MongoBaseEntity {

    @Indexed
    private String roomId;

    private String senderId;
    private String senderName;
    private String content;

    @Builder.Default
    private String type = "TEXT";

    @Builder.Default
    private Map<String, String> metadata = Map.of();
}
