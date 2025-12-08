package com.solarbookshop.orderservice;

import com.solarbookshop.orderservice.order.domain.Order;
import com.solarbookshop.orderservice.order.domain.OrderRepository;
import com.solarbookshop.orderservice.order.domain.OrderStatus;
import com.solarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
class OrderServiceApplicationTests {

  @Autowired
  private InputDestination inputDestination;

  @Autowired
  private OrderRepository orderRepository;

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
}
