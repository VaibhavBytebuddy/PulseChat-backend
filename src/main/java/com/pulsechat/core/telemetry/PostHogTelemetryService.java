package com.pulsechat.core.telemetry;

import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostHogTelemetryService {

    PostHogProperties properties;
    RestClient restClient;
    ExecutorService executor;

    public PostHogTelemetryService(PostHogProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.host() != null && !properties.host().isBlank() ? properties.host() : "https://us.i.posthog.com")
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void capture(String distinctId, String eventName, Map<String, Object> eventProperties) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            return;
        }

        executor.submit(() -> {
            try {
                Map<String, Object> enriched = new HashMap<>(eventProperties != null ? eventProperties : Map.of());
                enriched.put("$lib", "posthog-java-spring");
                enriched.put("environment", "development");

                Map<String, Object> payload = Map.of(
                    "api_key", properties.apiKey(),
                    "event", eventName,
                    "distinct_id", distinctId != null ? distinctId : "anonymous_backend",
                    "properties", enriched,
                    "timestamp", Instant.now().toString()
                );

                restClient.post()
                    .uri("/capture/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            } catch (Exception ex) {
                log.warn("Failed to dispatch PostHog event '{}': {}", eventName, ex.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
