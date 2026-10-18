package com.pulsechat.chat.repository;

import com.pulsechat.chat.model.ChatRoomEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoomEntity, String> {
    Optional<ChatRoomEntity> findByName(String name);
    boolean existsByName(String name);
}
