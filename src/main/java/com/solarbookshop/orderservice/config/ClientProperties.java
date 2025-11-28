package com.solarbookshop.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "solar")
public record ClientProperties(
        @NotNull
        URI catalogServiceUri,
        @NotNull
        Duration catalogServiceTimeout
) {
}
