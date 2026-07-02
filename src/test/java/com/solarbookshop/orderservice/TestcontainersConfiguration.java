package com.solarbookshop.orderservice;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
  }

  @Bean
  KeycloakContainer keycloakContainer() {
    return new KeycloakContainer("quay.io/keycloak/keycloak:26.6")
        .withRealmImportFile("test-realm-config.json");
  }

  @Bean
  DynamicPropertyRegistrar keycloakProperties(KeycloakContainer keycloak) {
    return registry -> registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloak.getAuthServerUrl() + "/realms/SolarBookshop");
  }
}
