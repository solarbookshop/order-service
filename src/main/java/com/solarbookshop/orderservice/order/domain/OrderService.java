package com.solarbookshop.orderservice.order.domain;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public Flux<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  public Mono<Order> submitOrder(String isbn, int quantity) {
    System.out.println("Submitting order for ISBN: " + isbn + " and quantity: " + quantity);
    return Mono.just(buildRejectedOrder(isbn, quantity))
            .flatMap(orderRepository::save);
  }

  private Order buildRejectedOrder(String isbn, int quantity) {
    return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
  }
}
