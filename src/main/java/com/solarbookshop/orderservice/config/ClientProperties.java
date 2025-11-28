package com.solarbookshop.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "solar")
public record ClientProperties(
        @NotNull
        URI catalogServiceUri
) {
}
