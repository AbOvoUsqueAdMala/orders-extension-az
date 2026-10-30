package com.example.consumer.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consumer_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumerOrder {
	@Id
	private UUID id;

	public ConsumerOrder(UUID id) {
		this.id = id;
	}
}
