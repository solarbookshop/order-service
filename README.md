# Order Service

This application is part of the Solar Bookshop system and provides the functionality for ordering books.

## REST API

| Endpoint | HTTP method | Request body | Status | Response body | Description         |
|----------|-------------|--------------|--------|---------------|---------------------|
| /orders  | GET         |              | 200    | Orders        | Get all the orders. |
| /orders  | POST        | OrderRequest | 200    | Order         | Submit a new order. |

![event-broker-diagram](/event-broker.svg)
> Order Service and [Dispatcher Service](https://github.com/solarbookshop/dispatcher-service) communicate asynchronously and indirectly by producing and consuming events that are collected and distributed by an event broker (RabbitMQ).

## Useful Commands

| Gradle Command             | Description                                   |
|:---------------------------|:----------------------------------------------|
| `./gradlew bootRun`        | Run the application.                          |
| `./gradlew build`          | Build the application.                        |
| `./gradlew test`           | Run tests.                                    |
| `./gradlew bootJar`        | Package the application as a JAR.             |
| `./gradlew bootBuildImage` | Package the application as a container image. |
