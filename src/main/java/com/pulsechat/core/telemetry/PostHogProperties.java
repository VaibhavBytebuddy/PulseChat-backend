package com.pulsechat.core.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.posthog")
@Validated
public record PostHogProperties(
    String apiKey,
    String host,
    boolean enabled,
    int batchSize,
    int flushIntervalMs
) {}
