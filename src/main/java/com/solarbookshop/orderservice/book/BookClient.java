package com.solarbookshop.orderservice.book;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class BookClient {
  private static final String BOOKS_ROOT_API = "/books/";
  private final WebClient webClient;
  private final Duration timeout;

  public BookClient(WebClient webClient, Duration timeout) {
    this.webClient = webClient;
    this.timeout = timeout;
  }

  public Mono<Book> getBookByIsbn(String isbn) {
    return webClient
            .get()
            .uri(BOOKS_ROOT_API + isbn)
            .retrieve()
            .bodyToMono(Book.class)
            .timeout(timeout, Mono.empty())
            .retryWhen(Retry.backoff(3, Duration.ofMillis(300)));
  }
}
