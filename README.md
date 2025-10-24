# apiktjava

Простейший REST‑сервис на Ktor c маршрутами `GET /items`, `POST /items` и `DELETE /items/{id}`, использующий JSON‑сериализацию через kotlinx.serialization.

## Требования
- JDK 21 (можно воспользоваться тем, что Gradle кладёт в `~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2`).

Чтобы не передавать `JAVA_HOME` каждый раз, добавьте в `gradle.properties` строку:
```
org.gradle.java.home=/home/xxx/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
```

## Запуск приложения
```
JAVA_HOME=/home/xxx/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew run
```
Сервер стартует на `http://localhost:8080`. После запуска попробуйте:
```
curl -s http://localhost:8080/items
```

## Запуск тестов
```
JAVA_HOME=/home/xxx/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew test
```
