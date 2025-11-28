package com.solarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BookClientTests {
  private MockWebServer mockWebServer;
  private BookClient bookClient;

  @BeforeEach
  void setUp() throws IOException {
    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();
    var webClient = WebClient.create(mockWebServer.url("/").url().toString());
    this.bookClient = new BookClient(webClient, Duration.ofSeconds(3));
  }

  @AfterEach
  void tearDown() throws IOException {
    this.mockWebServer.close();
  }

  @Test
  void when_book_exists_then_return_book() {
    var bookIsbn = "1234567890";
    var mockResponse = new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                    {
                      "isbn": %s,
                      "title": "Title",
                      "author": "Author",
                      "price": 9.90,
                      "publisher": "Polarsophia"
                    }
                    """.formatted(bookIsbn));
    mockWebServer.enqueue(mockResponse);

    Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

    StepVerifier.create(book)
            .expectNextMatches(b -> b.isbn().equals(bookIsbn))
            .verifyComplete();
  }
}