package com.example.extension.az.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "extension_az_orders", schema = "orders_extension_az")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AzOrder {
	@Id
	private UUID id;
	@Column(nullable = false, length = 200)
	private String description;
	@Column(nullable = false, length = 20)
	private String status;
	@Column(length = 200)
	private String externalId;
	@Column(columnDefinition = "text")
	private String payload;
	@Column(nullable = false)
	private Instant createdAt;
	@Column(nullable = false)
	private Instant updatedAt;
	@Version
	private long version;

	public AzOrder(String description) {
		this.id = UUID.randomUUID();
		this.description = description;
		this.status = "NEW";
		this.createdAt = Instant.now();
		this.updatedAt = createdAt;
	}

	public void complete(String status, String externalId, String payload) {
		this.status = status;
		this.externalId = externalId;
		this.payload = payload;
		this.updatedAt = Instant.now();
	}

	public void fail() {
		this.status = "FAILED";
		this.updatedAt = Instant.now();
	}
}
