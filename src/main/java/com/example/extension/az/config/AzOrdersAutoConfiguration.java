package com.example.extension.az.config;

import com.example.extension.az.service.AzOrderGateway;
import com.example.extension.az.service.AzOrderStateService;
import com.example.extension.az.service.AzOrderWorkflow;
import com.example.extension.az.tasks.AzOrderTasks;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@AutoConfiguration(after = DataSourceAutoConfiguration.class,
		before = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@AutoConfigurationPackage(basePackages = "com.example.extension.az")
@Import({AzOrderStateService.class, AzOrderWorkflow.class,
		AzOrderTasks.class})
public class AzOrdersAutoConfiguration {
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
