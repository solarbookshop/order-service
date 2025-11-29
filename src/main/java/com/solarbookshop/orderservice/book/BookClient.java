package com.solarbookshop.orderservice.book;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
            .uri(uri -> {
                var fullUri = uri.path(BOOKS_ROOT_API + isbn).build();
                System.out.println("Requesting: " + fullUri);
                return fullUri;
            })
            .retrieve()
            .bodyToMono(Book.class)
            .doOnNext(book -> System.out.println("Received response: " + book))
            .doOnError(error -> System.err.println("Error for ISBN " + isbn + ": " + error.getMessage()))
            .timeout(timeout, Mono.empty())
            .onErrorResume(WebClientResponseException.NotFound.class, _ -> Mono.empty())
            .retryWhen(Retry.backoff(3, Duration.ofMillis(300)))
            .onErrorResume(Exception.class, _ -> Mono.empty());
  }
}
