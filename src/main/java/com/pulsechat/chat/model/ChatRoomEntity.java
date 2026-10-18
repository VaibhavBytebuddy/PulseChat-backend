package com.pulsechat.chat.model;

import com.pulsechat.core.entity.MongoBaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Document(collection = "rooms")
public class ChatRoomEntity extends MongoBaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    @Builder.Default
    private long messageCount = 0;
}
