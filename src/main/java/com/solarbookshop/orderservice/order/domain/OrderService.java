package com.solarbookshop.orderservice.order.domain;

import com.solarbookshop.orderservice.book.Book;
import com.solarbookshop.orderservice.book.BookClient;
import com.solarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final BookClient bookClient;

  public OrderService(OrderRepository orderRepository, BookClient bookClient) {
    this.orderRepository = orderRepository;
    this.bookClient = bookClient;
  }

  public static Order buildRejectedOrder(String isbn, int quantity) {
    return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
  }

  public Flux<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  public Mono<Order> submitOrder(String isbn, int quantity) {
    return bookClient.getBookByIsbn(isbn)
            .map(book -> buildAcceptedOrder(book, quantity))
            .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
            .flatMap(orderRepository::save);
  }

  private Order buildAcceptedOrder(Book book, int quantity) {
    return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
  }

  public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
    return flux.flatMap(message -> orderRepository.findById(message.orderId()))
            .map(this::buildDispatchedOrder)
            .flatMap(orderRepository::save);
  }

  private Order buildDispatchedOrder(Order existingOrder) {
    return new Order(
            existingOrder.id(),
            existingOrder.bookIsbn(),
            existingOrder.bookName(),
            existingOrder.bookPrice(),
            existingOrder.quantity(),
            OrderStatus.DISPATCHED,
            existingOrder.createdDate(),
            existingOrder.lastModifiedDate(),
            existingOrder.version()
    );
  }
}
