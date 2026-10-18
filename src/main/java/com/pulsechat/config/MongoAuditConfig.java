package com.pulsechat.config;

import com.pulsechat.core.context.UserContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Optional;

@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorAware")
public class MongoAuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            String userId = UserContextHolder.getUserId();
            return Optional.ofNullable(userId != null && !userId.isBlank() ? userId : "system");
        };
    }
}
