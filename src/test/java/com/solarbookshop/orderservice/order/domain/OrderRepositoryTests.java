package com.solarbookshop.orderservice.order.domain;

import com.solarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Testcontainers
@Import({DataConfig.class})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OrderRepositoryTests {
  @Container
  private static PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:18-alpine");
  @Autowired
  private OrderRepository orderRepository;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    var r2dbcUrl = String.format("r2dbc:postgresql://%s:%s/%s",
            postgresql.getHost(),
            postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgresql.getDatabaseName());
    registry.add("spring.r2dbc.url", () -> r2dbcUrl);
    registry.add("spring.r2dbc.username", postgresql::getUsername);
    registry.add("spring.r2dbc.password", postgresql::getPassword);
    registry.add("spring.flyway.url", postgresql::getJdbcUrl);
  }

  @Test
  void create_rejected_order() {
    var rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3);
    StepVerifier
            .create(orderRepository.save(rejectedOrder))
            .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
            .verifyComplete();
  }
}