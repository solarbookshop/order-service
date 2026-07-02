package com.solarbookshop.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
  @Bean
  SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(exchangeSpec -> exchangeSpec.anyExchange().authenticated())
        .oauth2ResourceServer(serverSpec -> serverSpec.jwt(Customizer.withDefaults()))
        .requestCache(requestCacheSpec -> requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
  }
}
