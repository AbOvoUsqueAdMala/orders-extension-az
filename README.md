# Orders Extension AZ

Независимая Java 21 / Spring Boot 3.5.16 библиотека с JPA, Flyway, RestClient
и db-scheduler 16.12.0. Версия Java API — `2.0.0-SNAPSHOT`.
Библиотека не содержит REST-контроллеров и не поднимает веб-сервер.
Она не зависит от демо-приложения, `orders-application-api`, адаптеров или MapStruct.

## Подключение

```shell
mvn clean install
```

В POM совместимого Spring Boot-приложения:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>orders-extension-az</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

Для других машин установите артефакт там либо опубликуйте JAR и POM через `mvn deploy`
в Maven-репозиторий вашей организации. GitHub хранит исходники и сам по себе JAR не публикует.

## Java API и собственные модели

Spring Boot AutoConfiguration регистрирует `AzOrderStateService`, обработчики задач,
HTTP gateway, JPA-компоненты и собственный Flyway initializer.
Вызывающее приложение использует типизированную команду:

```java
var created = orderStateService.create(new AzOrderCommand("Заказ AZ"));
var current = orderStateService.get(created.id());
```

Тип сервиса: `com.example.extension.az.service.AzOrderStateService`.
DTO находятся в `com.example.extension.az.model.dto`.

- `AzOrderCommand` содержит одно поле `details`, от 1 до 200 символов, не только пробелы.
- `AzOrderView` содержит `id`, `details`, `status`, `externalId`, `payload`, время и историю.
- Неизвестный UUID приводит к `NoSuchElementException`; вызывающее приложение определяет HTTP-ответ.

В демо маппинг `OrderSubmission.description ↔ AzOrderCommand.details / AzOrderView.details`
выполняет MapStruct внутри `demo-az-adapter` в репозитории главного сервиса.
Поля `customerReference` и `internalComment` остаются в главном сервисе.
В другом проекте можно создать собственный адаптер или напрямую вызвать этот Java API.

## Инфраструктура и выполнение

Нужны PostgreSQL DataSource, JDBC-драйвер и общая таблица db-scheduler,
созданная миграцией приложения. SQL находится в `docs/scheduled_tasks.sql`.
Имя очереди задаёт `db-scheduler.table-name`; библиотека не создаёт отдельный Scheduler.
Штатная Boot-настройка JPA расширяется пакетом библиотеки. При собственных `@EntityScan`
или `@EnableJpaRepositories` явно включите пакеты её Entity и Repository.

Цепочка: `AZ_REGISTER_ORDER → AZ_GET_ORDER → AZ_SEND_ORDER`.
Создание заказа и первой задачи атомарно в JPA-транзакции; HTTP выполняется вне неё.
Следующий шаг, результат и состояние сохраняются одной короткой транзакцией.
Используются `scheduleIfNotExists`, блокировка заказа и проверка ожидаемого состояния.
Очередь, потоки, повторы и восстановление принадлежат db-scheduler.

Статусы: `NEW → REGISTERED → FETCHED → SENT` или `FAILED`.
HTTP `408`, `429`, `5xx`, ошибки соединения и timeout повторяются; остальные `4xx`
и некорректный ответ завершают заказ ошибкой. По умолчанию пять повторов с backoff 2, 4, 8, 16, 32 секунды.
После исчерпания попыток задача удаляется, история остаётся.
Настройки: `orders.retry-delay`, `orders.max-retries`, `orders.extension.az.base-url`.
URL по умолчанию `http://localhost:8081`, connect/read timeout — 2/5 секунд.

Передаётся стабильный `Idempotency-Key: <taskName>:<UUID>`. Внешняя система должна обеспечивать
дедупликацию: rollback PostgreSQL не отменяет уже выполненный внешний HTTP-вызов.

## Миграции и совместимость

Миграции: `classpath:orders-extension-az/db/migration`.
Схема: `orders_extension_az`, таблица истории: `orders_extension_az.az_schema_history`.
Миграции не попадают в стандартный `classpath:db/migration` вызывающего приложения.
Отдельный initializer сохраняет штатный Flyway потребителя и не заменяет его bean.

В версии 2 удалены контроллер и прежний request DTO, метод `create(String)` заменён
на `create(AzOrderCommand)`, поле `description` в DTO переименовано в `details`.
SQL-колонки, схемы, история миграций и имена задач сохранены: существующие данные
и ожидающие задачи совместимы. Демо требует эту новую версию API через свой адаптер.

## Проверки

```shell
mvn clean verify
```

Docker обязателен. Testcontainers поднимает PostgreSQL, WireMock имитирует внешнюю систему.
Независимое тестовое приложение расположено вне пакета библиотеки и запускается без веб-сервера.
Проверяются сохранение Flyway/JPA-компонентов потребителя, один Scheduler,
отсутствие REST-контроллеров и выполнение цепочки через Java API.
