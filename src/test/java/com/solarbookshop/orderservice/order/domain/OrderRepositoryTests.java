package com.solarbookshop.orderservice.order.domain;

import com.solarbookshop.orderservice.TestcontainersConfiguration;
import com.solarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.test.StepVerifier;

import java.util.Objects;

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

  @Test
  void creating_order_without_authentication_creates_no_audit_meta_data() {
    var rejectedOrder = OrderService.buildRejectedOrder("1234567810", 3);
    StepVerifier
        .create(orderRepository.save(rejectedOrder))
        .expectNextMatches(order -> Objects.isNull(order.createdBy()) && Objects.isNull(order.lastModifiedBy()))
        .verifyComplete();
  }

  @Test
  @WithMockUser(username = "ram")
  void creating_order_with_authentication_creates_audit_meta_data() {
    var rejectedOrder = OrderService.buildRejectedOrder("1234567820", 3);
    StepVerifier
        .create(orderRepository.save(rejectedOrder))
        .expectNextMatches(order -> order.createdBy().equals("ram") && order.lastModifiedBy().equals("ram"))
        .verifyComplete();
  }
}