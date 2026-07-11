plugins {
  java
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "order-service"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

extra["springCloudVersion"] = "2025.1.2"
extra["testKeyCloakVersion"] = "4.2.1"
extra["okHttpVersion"] = "5.4.0"
extra["otelVersion"] = "2.29.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
  implementation("org.springframework.cloud:spring-cloud-stream-binder-rabbit")
  implementation("org.springframework.cloud:spring-cloud-starter-config")
  implementation("org.flywaydb:flyway-database-postgresql")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.postgresql:r2dbc-postgresql")
  runtimeOnly("io.micrometer:micrometer-registry-prometheus")
  runtimeOnly("io.opentelemetry.javaagent:opentelemetry-javaagent:${property("otelVersion")}")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
  testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter")
  testImplementation("org.testcontainers:testcontainers-postgresql")
  testImplementation("org.testcontainers:testcontainers-r2dbc")
  testImplementation("com.squareup.okhttp3:mockwebserver3")
  testImplementation("com.github.dasniko:testcontainers-keycloak:${property("testKeyCloakVersion")}")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    mavenBom("com.squareup.okhttp3:okhttp-bom:${property("okHttpVersion")}")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.bootBuildImage {
  builder.set("paketobuildpacks/builder-noble-java-tiny:latest")
  imageName.set(project.name)
  environment.set(mapOf("BP_JVM_VERSION" to "25"))
}
