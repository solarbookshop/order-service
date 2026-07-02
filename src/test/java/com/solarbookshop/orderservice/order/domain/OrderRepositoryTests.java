package com.solarbookshop.orderservice.order.domain;

import com.solarbookshop.orderservice.TestcontainersConfiguration;
import com.solarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({DataConfig.class, TestcontainersConfiguration.class})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OrderRepositoryTests {
  @Autowired
  private OrderRepository orderRepository;

  @Test
  void create_rejected_order() {
    var rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3);
    StepVerifier
            .create(orderRepository.save(rejectedOrder))
            .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
            .verifyComplete();
  }
}