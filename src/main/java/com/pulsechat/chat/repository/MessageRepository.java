package com.pulsechat.chat.repository;

import com.pulsechat.chat.model.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<MessageEntity, String> {

    List<MessageEntity> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    List<MessageEntity> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        String roomId,
        Instant cursor,
        Pageable pageable
    );

    long countByRoomId(String roomId);
}
