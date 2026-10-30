# Orders Extension AZ

Самостоятельная Maven-библиотека для Spring Boot 3.5.x и Java 21.
Репозиторий: [AbOvoUsqueAdMala/orders-extension-az](https://github.com/AbOvoUsqueAdMala/orders-extension-az).
Родительский POM — Spring Boot; библиотека не зависит от `demo-project` и другой
страновой библиотеки. Версия задаётся в её собственном `pom.xml`.

## Сборка и локальное подключение

Нужны JDK 21, Maven и Docker для интеграционных тестов.

```shell
git clone https://github.com/AbOvoUsqueAdMala/orders-extension-az.git
cd orders-extension-az
mvn clean install
```

В другом Maven-проекте добавьте обычную зависимость:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>orders-extension-az</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

После `install` артефакт доступен в локальном репозитории `.m2`.
Это обычный JAR библиотеки, без Spring Boot repackage и без классов тестового приложения.
Git submodule для подключения зависимости к другому приложению не нужен.

При изменении библиотеки в отдельном checkout сначала выполните в ней
`mvn clean install`, затем пересоберите и перезапустите приложение-потребитель.
`-am` собирает только зависимости, входящие в текущий Maven reactor: для библиотеки
из отдельного репозитория вне reactor он не заменяет `install` или публикацию.

## Требования к приложению-потребителю

- Spring Boot 3.5.x, Java 21, Spring MVC, PostgreSQL DataSource и JDBC-драйвер PostgreSQL.
- Одна общая таблица db-scheduler 16.12.0. Её создаёт приложение-потребитель своей
  Flyway-миграцией; готовая схема находится в [docs/scheduled_tasks.sql](docs/scheduled_tasks.sql).
- Транзакции JPA и scheduler должны использовать один DataSource и
  `JpaTransactionManager`; стартер использует `TransactionAwareDataSourceProxy`.
- Внешняя HTTP-система, реализующая контракт регистрации, получения и отправки заказа.

Минимальные настройки:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders
    username: orders
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
db-scheduler:
  table-name: scheduled_tasks
  threads: 4
  polling-interval: 1s
  delay-startup-until-context-ready: true
  immediate-execution-enabled: false
orders:
  extension:
    az:
      base-url: http://localhost:8081
  retry-delay: 2s
  max-retries: 5
```

JPA, MVC, Flyway и db-scheduler starter приходят транзитивно. Драйвер добавьте в
приложение с runtime scope:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Автоконфигурация регистрирует сервисы, контроллер, Entity, Repository и три
обработчика без ручного `@ComponentScan` или `@Import`.
Она добавляет свой корневой пакет к стандартным пакетам Spring Boot, сохраняя
обычное обнаружение Entity и Repository приложения. Если приложение вручную
ограничивает сканирование через `@EntityScan` или `@EnableJpaRepositories`,
включите также пакеты `com.example.extension.az.model.entity` и
`com.example.extension.az.repository` соответственно.

## Миграции и совместная работа

Собственные миграции находятся в `classpath:orders-extension-az/db/migration`,
отдельно от стандартного `classpath:db/migration` приложения.
Схема библиотеки — `orders_extension_az`, таблица истории —
`az_schema_history`.
Библиотека не подменяет Flyway приложения; все инициализаторы завершаются до
инициализации Hibernate и запуска общего scheduler.

KZ и AZ можно подключать вместе: у них разные Java-пакеты, схемы, имена задач и
Spring-компоненты. Один экземпляр db-scheduler исполняет все цепочки.
Удаление зависимости не удаляет уже существующие таблицы; перед отключением
дождитесь завершения её задач.


## REST и HTTP-контракт

- `POST /api/extension/az/orders`, тело `{"description":"Demo order"}` → `202 Accepted`.
- `GET /api/extension/az/orders/{id}` → состояние заказа и история.
- Цепочка: `AZ_REGISTER_ORDER → AZ_GET_ORDER → AZ_SEND_ORDER`.
- Регистрация: `POST /az/source/orders`, JSON `id` и `description`;
  ответ `{"externalId":"..."}`.
- Получение: `GET /az/source/orders/{externalId}`, ответ JSON заказа.
- Отправка: `POST /az/destination/orders`, тело полученного заказа.

POST-запросы используют стабильный `Idempotency-Key`; реальный получатель должен
выполнять дедупликацию. HTTP и PostgreSQL не образуют распределённую транзакцию.
Повторы временных ошибок выполняет db-scheduler с ограничением и backoff.
Демо с WireMock находится в [demo-lib-tasks](https://github.com/AbOvoUsqueAdMala/demo-lib-tasks).

## Публикация Maven-артефакта

Исходный GitHub-репозиторий сам по себе не является Maven-репозиторием.
Для других машин опубликуйте JAR и POM в Maven-репозиторий, например Nexus или
Artifactory, используя `mvn deploy` и `distributionManagement` вашей организации.
Учетные данные храните в Maven `settings.xml` или секретах CI, не в POM.

Координаты `com.example` используются для демо. Перед публикацией в общий реестр
выберите принадлежащий вам groupId. Для стабильного выпуска задайте новую
неснапшотную версию и используйте её в зависимостях потребителей.

## Проверки

`mvn clean verify` запускает `StandaloneExtensionIT` с Testcontainers PostgreSQL
и WireMock. Тестовое приложение расположено вне пакета библиотеки и содержит свои
Entity, Repository и стандартную Flyway-миграцию. Проверяются их совместная
инициализация и полная цепочка через REST без конфигурации из демо.
