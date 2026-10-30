package com.example.extension.az.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "extension_az_order_task_results", schema = "orders_extension_az")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AzTaskResult {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private UUID orderId;
	@Column(nullable = false, length = 40)
	private String taskName;
	@Column(nullable = false, length = 20)
	private String outcome;
	@Column(columnDefinition = "text")
	private String response;
	@Column(length = 500)
	private String error;
	@Column(nullable = false)
	private Instant performedAt;

	public AzTaskResult(UUID orderId, String taskName, String outcome,
			String response, String error) {
		this.orderId = orderId;
		this.taskName = taskName;
		this.outcome = outcome;
		this.response = response;
		this.error = error;
		this.performedAt = Instant.now();
	}
}
