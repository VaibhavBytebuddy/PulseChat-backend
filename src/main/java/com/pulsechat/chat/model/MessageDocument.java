package com.pulsechat.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "room_created_desc_idx", def = "{'roomId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
})
public class MessageDocument {

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String senderId;
    private String senderName;
    private String content;

    @Builder.Default
    private String type = "TEXT";

    @Builder.Default
    private Map<String, String> metadata = Map.of();

    @CreatedDate
    private Instant createdAt;

    @Version
    private Long version;

    public MessageDocument(String roomId, String senderId, String senderName, String content, String type, Map<String, String> metadata) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.type = (type != null && !type.isBlank()) ? type : "TEXT";
        this.metadata = metadata != null ? metadata : Map.of();
    }
}
