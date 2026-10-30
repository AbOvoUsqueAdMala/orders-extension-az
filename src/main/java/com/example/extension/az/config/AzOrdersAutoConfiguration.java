package com.example.extension.az.config;

import com.example.extension.az.controller.AzOrderController;
import com.example.extension.az.model.entity.AzOrder;
import com.example.extension.az.repository.AzOrderRepository;
import com.example.extension.az.service.AzOrderGateway;
import com.example.extension.az.service.AzOrderStateService;
import com.example.extension.az.service.AzOrderWorkflow;
import com.example.extension.az.tasks.AzOrderTasks;
import java.net.http.HttpClient;
import java.time.Duration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@AutoConfiguration(after = DataSourceAutoConfiguration.class,
		before = {HibernateJpaAutoConfiguration.class, FlywayAutoConfiguration.class})
@EntityScan(basePackageClasses = AzOrder.class)
@EnableJpaRepositories(basePackageClasses = AzOrderRepository.class)
@Import({AzOrderStateService.class, AzOrderWorkflow.class,
		AzOrderController.class, AzOrderTasks.class})
public class AzOrdersAutoConfiguration {
	@Bean
	Flyway azFlyway(DataSource dataSource) {
		return Flyway.configure().dataSource(dataSource)
				.schemas("orders_extension_az").defaultSchema("orders_extension_az")
				.table("az_schema_history")
				.locations("classpath:db/migration/extension/az").load();
	}

	@Bean
	FlywayMigrationInitializer azFlywayInitializer(
			@Qualifier("azFlyway") Flyway flyway) {
		return new FlywayMigrationInitializer(flyway);
	}

	@Bean
	AzOrderGateway azOrderGateway(RestClient.Builder builder,
			@Value("${orders.extension.az.base-url:http://localhost:8081}") String baseUrl) {
		var httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(Duration.ofSeconds(2)).build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		return new AzOrderGateway(builder.clone().baseUrl(baseUrl)
				.requestFactory(requestFactory).build());
	}
}
