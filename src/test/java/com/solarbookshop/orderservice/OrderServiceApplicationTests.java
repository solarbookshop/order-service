package com.solarbookshop.orderservice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.solarbookshop.orderservice.book.Book;
import com.solarbookshop.orderservice.book.BookClient;
import com.solarbookshop.orderservice.order.domain.Order;
import com.solarbookshop.orderservice.order.domain.OrderRepository;
import com.solarbookshop.orderservice.order.domain.OrderStatus;
import com.solarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.solarbookshop.orderservice.order.event.OrderDispatchedMessage;
import com.solarbookshop.orderservice.order.web.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows non-static @BeforeAll
@AutoConfigureWebTestClient
class OrderServiceApplicationTests {
  // Customer
  KeycloakToken ramTokens;
  // Customer and employee
  KeycloakToken shayamTokens;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  KeycloakContainer keycloakContainer;

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  InputDestination inputDestination;

  @Autowired
  OutputDestination outputDestination;

  @Autowired
  OrderRepository orderRepository;

  @MockitoBean
  BookClient bookClient;

  @BeforeAll
  void generateAccessTokens() {
    var webClient = WebClient.builder()
        .baseUrl(keycloakContainer.getAuthServerUrl() + "/realms/SolarBookshop/protocol/openid-connect/token")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build();

    shayamTokens = authenticateWith("shayam", "password", webClient);
    ramTokens = authenticateWith("ram", "password", webClient);
  }

  @Test
  void when_post_request_and_book_exists_then_order_accepted() {
    String bookIsbn = "1234567899";
    var book = new Book(bookIsbn, "Title", "Author", 9.90);
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    var createdOrder = webTestClient.post().uri("/orders")
        .headers(headers -> headers.setBearerAuth(ramTokens.accessToken()))
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class)
        .value(order -> {
          assertThat(order.bookIsbn()).isEqualTo(orderRequest.isbn());
          assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
          assertThat(order.bookName()).isEqualTo(book.title() + " - " + book.author());
          assertThat(order.bookPrice()).isEqualTo(book.price());
          assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
        })
        .returnResult().getResponseBody();

    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
        .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
  }

  @Test
  void when_post_request_and_book_not_exists_then_order_rejected() {
    String bookIsbn = "1234567894";
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    webTestClient.post().uri("/orders")
        .headers(headers -> headers.setBearerAuth(ramTokens.accessToken()))
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class)
        .value(order -> {
          assertThat(order.bookIsbn()).isEqualTo(orderRequest.isbn());
          assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
          assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        });
  }

  @Test
  void when_order_dispatched_event_received_then_order_status_is_updated() {
    // Given: Create an accepted order in the database
    var acceptedOrder = Order.of("1234567890", "Test Book - Test Author", 9.99, 1, OrderStatus.ACCEPTED);

    Long orderId = orderRepository.save(acceptedOrder)
        .map(Order::id)
        .block();

    assertThat(orderId).isNotNull();

    // When: Send an OrderDispatchedMessage event
    var orderDispatchedMessage = new OrderDispatchedMessage(orderId);
    var message = MessageBuilder.withPayload(orderDispatchedMessage).build();
    inputDestination.send(message);

    // Then: Verify the order status is updated to DISPATCHED
    // Give some time for the async processing to complete
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    StepVerifier.create(orderRepository.findById(orderId))
        .assertNext(order -> {
          assertThat(order.status()).isEqualTo(OrderStatus.DISPATCHED);
          assertThat(order.bookIsbn()).isEqualTo("1234567890");
          assertThat(order.bookName()).isEqualTo("Test Book - Test Author");
        })
        .verifyComplete();
  }

  @Test
  void when_multiple_order_dispatched_events_received_then_all_orders_are_updated() {
    // Given: Create multiple accepted orders
    var order1 = Order.of("1111111111", "Book One - Author One", 10.99, 1, OrderStatus.ACCEPTED);
    var order2 = Order.of("2222222222", "Book Two - Author Two", 12.99, 2, OrderStatus.ACCEPTED);

    Long orderId1 = orderRepository.save(order1).map(Order::id).block();
    Long orderId2 = orderRepository.save(order2).map(Order::id).block();

    assertThat(orderId1).isNotNull();
    assertThat(orderId2).isNotNull();

    // When: Send multiple OrderDispatchedMessage events
    var message1 = MessageBuilder.withPayload(new OrderDispatchedMessage(orderId1)).build();
    var message2 = MessageBuilder.withPayload(new OrderDispatchedMessage(orderId2)).build();

    inputDestination.send(message1);
    inputDestination.send(message2);

    // Then: Verify both orders are updated to DISPATCHED
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    StepVerifier.create(orderRepository.findById(orderId1))
        .assertNext(order -> assertThat(order.status()).isEqualTo(OrderStatus.DISPATCHED))
        .verifyComplete();

    StepVerifier.create(orderRepository.findById(orderId2))
        .assertNext(order -> assertThat(order.status()).isEqualTo(OrderStatus.DISPATCHED))
        .verifyComplete();
  }

  @Test
  void when_order_dispatched_event_for_non_existent_order_then_no_error() {
    // Given: A non-existent order ID
    Long nonExistentOrderId = 999999L;

    // When: Send an OrderDispatchedMessage for non-existent order
    var message = MessageBuilder.withPayload(new OrderDispatchedMessage(nonExistentOrderId)).build();
    inputDestination.send(message);

    // Then: Verify no exception is thrown and processing completes
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Verify the order doesn't exist
    StepVerifier.create(orderRepository.findById(nonExistentOrderId))
        .expectNextCount(0)
        .verifyComplete();
  }

  private KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
    return webClient
        .post()
        .body(BodyInserters.fromFormData("grant_type", "password")
            .with("client_id", "solar-test")
            .with("username", username)
            .with("password", password)
        )
        .retrieve()
        .bodyToMono(KeycloakToken.class)
        .block();
  }

  private record KeycloakToken(String accessToken) {
    @JsonCreator
    private KeycloakToken(@JsonProperty("access_token") final String accessToken) {
      this.accessToken = accessToken;
    }
  }
}
