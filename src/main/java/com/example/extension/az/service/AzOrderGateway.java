package com.example.extension.az.service;

import com.example.extension.az.model.dto.AzOrderView;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class AzOrderGateway {
	private final RestClient restClient;

	public String register(AzOrderView customerOrder, String idempotencyKey) {
		var registration = restClient.post().uri("/az/source/orders")
				.header("Idempotency-Key", idempotencyKey)
				.body(Map.of("id", customerOrder.id(), "description", customerOrder.details()))
				.retrieve().body(Registration.class);
		if (registration == null || registration.externalId() == null
				|| registration.externalId().isBlank() || registration.externalId().length() > 200) {
			throw new IllegalArgumentException("Invalid registration response");
		}
		return registration.externalId();
	}

	public String fetch(String externalId) {
		String payload = restClient.get().uri("/az/source/orders/{id}", externalId)
				.retrieve().body(String.class);
		if (payload == null || payload.isBlank()) {
			throw new IllegalArgumentException("Empty order response");
		}
		return payload;
	}

	public String send(String payload, String idempotencyKey) {
		restClient.post().uri("/az/destination/orders")
				.header("Idempotency-Key", idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().toBodilessEntity();
		return "Delivered";
	}

	public record Registration(String externalId) {}
}
