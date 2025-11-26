# Order Service

This application is part of the Solar Bookshop system and provides the functionality for ordering books.

## REST API

| Endpoint | HTTP method | Request body | Status | Response body | Description         |
|----------|-------------|--------------|--------|---------------|---------------------|
| /orders  | GET         |              | 200    | Orders        | Get all the orders. |
| /orders  | POST        | OrderRequest | 200    | Order         | Submit a new order. |

## Useful Commands

| Gradle Command             | Description                                   |
|:---------------------------|:----------------------------------------------|
| `./gradlew bootRun`        | Run the application.                          |
| `./gradlew build`          | Build the application.                        |
| `./gradlew test`           | Run tests.                                    |
| `./gradlew bootJar`        | Package the application as a JAR.             |
| `./gradlew bootBuildImage` | Package the application as a container image. |
