package com.pulsechat.chat.repository;

import com.pulsechat.chat.model.ChatRoomDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoomDocument, String> {
    Optional<ChatRoomDocument> findByName(String name);
    boolean existsByName(String name);
}
