package com.solarbookshop.orderservice.order.web;

import com.solarbookshop.orderservice.order.domain.Order;
import com.solarbookshop.orderservice.order.domain.OrderService;
import com.solarbookshop.orderservice.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OrderControllerTests {
  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private OrderService orderService;

  @Test
  void when_book_not_available_then_reject_order() {
    var orderRequest = new OrderRequest("1234567890", 3);
    var expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());
    given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
            .willReturn(Mono.just(expectedOrder));

    webTestClient
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(Order.class).value(actualOrder -> {
              assertThat(actualOrder).isNotNull();
              assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
            });
  }
}