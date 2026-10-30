package com.example.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.consumer.model.entity.ConsumerOrder;
import com.example.consumer.repository.ConsumerOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@SpringBootTest(classes = ConsumerApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandaloneExtensionIT {
	@Container
	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:17.6-alpine");
	private static final WireMockServer WIREMOCK = wiremock();

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private ConsumerOrderRepository consumerOrderRepository;
	@Autowired
	private JdbcTemplate database;
	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry properties) {
		properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		properties.add("spring.datasource.username", POSTGRES::getUsername);
		properties.add("spring.datasource.password", POSTGRES::getPassword);
		properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		properties.add("spring.jpa.open-in-view", () -> "false");
		properties.add("db-scheduler.polling-interval", () -> "100ms");
		properties.add("db-scheduler.threads", () -> "2");
		properties.add("orders.extension.az.base-url", WIREMOCK::baseUrl);
		properties.add("logging.level.root", () -> "WARN");
	}

	@AfterAll
	static void stopWiremock() {
		WIREMOCK.stop();
	}

	@Test
	void preservesConsumerMigrationsAndJpaComponents() {
		UUID orderId = UUID.randomUUID();
		consumerOrderRepository.save(new ConsumerOrder(orderId));
		assertThat(consumerOrderRepository.findById(orderId)).isPresent();
		assertThat(applicationContext.getBeansOfType(Scheduler.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(Task.class)).hasSize(3);
		assertThat(applicationContext.getBeansOfType(Flyway.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(FlywayMigrationInitializer.class)).hasSize(2);
		assertThat(database.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void automaticallyRegistersAndExecutesLibraryChain() {
		RestClient client = RestClient.create("http://localhost:" + port);
		var response = client.post().uri("/api/extension/az/orders")
				.body(Map.of("description", "Order from independent consumer"))
				.retrieve().toEntity(JsonNode.class);
		assertThat(response.getStatusCode().value()).isEqualTo(202);
		String location = response.getHeaders().getLocation().toString();
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
			JsonNode order = client.get().uri(location).retrieve().body(JsonNode.class);
			assertThat(order.path("status").asText()).isEqualTo("SENT");
			assertThat(order.path("history").size()).isEqualTo(3);
			assertThat(order.path("history").get(0).path("taskName").asText())
					.isEqualTo("AZ_REGISTER_ORDER");
		});
	}

	private static WireMockServer wiremock() {
		var server = new WireMockServer(wireMockConfig().dynamicPort().globalTemplating(true));
		server.start();
		server.stubFor(post(urlEqualTo("/az/source/orders")).willReturn(aResponse()
				.withStatus(201).withHeader("Content-Type", "application/json")
				.withBody("{\"externalId\":\"{{jsonPath request.body '$.id'}}\"}")
				.withTransformers("response-template")));
		server.stubFor(get(urlPathMatching("/az/source/orders/.*")).willReturn(aResponse()
				.withHeader("Content-Type", "application/json")
				.withBody("{\"id\":\"{{request.pathSegments.[3]}}\"}")
				.withTransformers("response-template")));
		server.stubFor(post(urlEqualTo("/az/destination/orders"))
				.willReturn(aResponse().withStatus(204)));
		return server;
	}
}
