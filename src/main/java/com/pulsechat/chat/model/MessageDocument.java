package com.pulsechat.chat.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

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
    private String type = "TEXT";

    private Map<String, String> metadata = Map.of();

    @CreatedDate
    private Instant createdAt;

    @Version
    private Long version;

    public MessageDocument() {}

    public MessageDocument(String roomId, String senderId, String senderName, String content, String type, Map<String, String> metadata) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.type = type;
        this.metadata = metadata != null ? metadata : Map.of();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
