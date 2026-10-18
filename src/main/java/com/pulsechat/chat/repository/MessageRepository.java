package com.pulsechat.chat.repository;

import com.pulsechat.chat.model.MessageDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<MessageDocument, String> {

    List<MessageDocument> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    List<MessageDocument> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        String roomId,
        Instant cursor,
        Pageable pageable
    );

    long countByRoomId(String roomId);
}
