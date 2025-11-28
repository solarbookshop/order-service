package com.solarbookshop.orderservice.order.domain;

import com.solarbookshop.orderservice.book.Book;
import com.solarbookshop.orderservice.book.BookClient;
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

  public static Order buildRejectedOrder(String isbn, int quantity) {
    return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
  }
}
