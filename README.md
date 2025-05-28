# ValorRise Bot Service

A Spring Boot microservice for automating game play by interacting with a remote Game API. It selects tasks, manages in‑game purchases, tracks reputation, and optimizes score using Resilience4j for retries and Feign for HTTP client.

---

## Table of Contents

* [Features](#features)
* [Prerequisites](#prerequisites)
* [Getting Started](#getting-started)

  * [Clone Repository](#clone-repository)
  * [Configuration](#configuration)
  * [Build & Run](#build--run)
* [Testing](#testing)
* [Code Coverage](#code-coverage)
* [Project Structure](#project-structure)
* [Dependencies](#dependencies)
* [Logging](#logging)
* [Troubleshooting](#troubleshooting)
* [License](#license)

---

## Features

* **Game Play Automation**: Starts games, selects and solves tasks, purchases upgrades and healing.
* **Task Selection**: Decodes encrypted advertisements, filters out traps, scores by `reward × probability`.
* **Shop Integration**: Buys health potions and upgrades based on in-game gold and lives.
* **Reputation Tracking**: Fetches and logs reputation (people, state, underworld) every 5 turns.
* **Resilience**: Uses Resilience4j Retry for API stability.
* **Feign Client**: `GameApiClient` for remote calls, with Jackson JSON mapping.
* **Validation & Lombok**: Input validation and boilerplate reduction.

---

## Prerequisites

* **Java 17**
* **Gradle Wrapper** (included)
* **Maven Central** access for dependencies
* **Internet** (to reach the Game API)

---

## Getting Started

### Clone Repository

```bash
git clone https://github.com/rutar/ValorRise.git
cd ValorRise
```

### Configuration

Copy and customize `src/main/resources/application.yml` (or `.properties`):

```yaml
spring:
  application:
    name: ValorRise

api:
  base-url: https://dragonsofmugloar.com/api/v2  # Base URL of Game API

shop:
  min-lives-to-buy: 3               # Minimum lives threshold to consider buying a potion
  min-gold-to-buy: 50               # Minimum gold threshold to preserve after purchase

resilience4j:
  retry:
    instances:
      gameApi:
        max-attempts: 3
        wait-duration: 1s
```


### Build & Run

Use Gradle wrapper for consistent builds:

```bash
# Build project
./gradlew clean build

# Run application
./gradlew bootRun
```

Alternatively, generate a runnable JAR:

```bash
./gradlew bootJar
java -jar build/libs/valorise-bot-service-0.0.1-SNAPSHOT.jar
```

---

## Testing

Execute unit tests:

```bash
./gradlew test
```

Test reports are generated under `build/reports/tests/`.

---


## Project Structure

```
└─ com.valorrise.bot
   ├─ api.client         # Feign interfaces for Game API
   ├─ configuration      # API and shop configuration classes
   ├─ exception          # Custom exception types
   ├─ model.domain       # Core domain entities (Game, Advertisement, Reputation)
   ├─ model.dto          # Data Transfer Objects for API interaction
   ├─ model.mapper       # Mappers between DTOs and entities
   └─ service            # Business logic (GameService, TaskSelectionService, ShopService)
```

---

## Dependencies

* **Spring Boot 3.3.4**
* **Spring Cloud OpenFeign**
* **Resilience4j Spring Boot 3**
* **Jackson Databind**
* **Project Lombok**
* **JUnit Jupiter & Mockito**
* **JaCoCo**

See `build.gradle` for full dependency list.

---

## Logging

Logs are configured by Spring Boot defaults (Logback). Adjust in `src/main/resources/logback-spring.xml` if needed. Key loggers:

* `com.valorrise.bot.service.GameService`
* `com.valorrise.bot.service.TaskSelectionService`
* `com.valorrise.bot.service.ShopService`

---

## Troubleshooting

* **API Connection Errors**: Check `api.base-url`, network connectivity, and retry settings.
* **Insufficient Gold/Lives**: Adjust `shop.minLivesToBuy` and `shop.minGoldToBuy` thresholds.
* **Test Failures**: Run `./gradlew test --stacktrace` to diagnose.

---

## License

Distributed under the MIT License. See `LICENSE` for details.
