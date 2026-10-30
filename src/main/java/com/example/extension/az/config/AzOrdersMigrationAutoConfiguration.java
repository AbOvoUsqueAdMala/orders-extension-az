package com.example.extension.az.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = FlywayAutoConfiguration.class)
public class AzOrdersMigrationAutoConfiguration {
	@Bean
	FlywayMigrationInitializer azFlywayInitializer(DataSource dataSource) {
		Flyway flyway = Flyway.configure().dataSource(dataSource)
				.schemas("orders_extension_az").defaultSchema("orders_extension_az")
				.table("az_schema_history")
				.locations("classpath:orders-extension-az/db/migration").load();
		return new FlywayMigrationInitializer(flyway);
	}
}
