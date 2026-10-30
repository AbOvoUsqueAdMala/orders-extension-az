package com.example.extension.az.service;

import com.example.extension.az.model.dto.AzOrderView;
import com.example.extension.az.tasks.AzOrderStep;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@RequiredArgsConstructor
public class AzOrderWorkflow {
	private final AzOrderStateService orderStateService;
	private final AzOrderGateway orderGateway;

	public void execute(UUID orderId, AzOrderStep step) {
		AzOrderView customerOrder = orderStateService.get(orderId);
		if (!customerOrder.status().equals(step.expectedStatus())) {
			return;
		}
		try {
			perform(customerOrder, step);
		} catch (RestClientResponseException failure) {
			boolean retryable = failure.getStatusCode().is5xxServerError()
					|| failure.getStatusCode().value() == 429
					|| failure.getStatusCode().value() == 408;
			orderStateService.recordFailure(orderId, step,
					"HTTP " + failure.getStatusCode().value(), !retryable);
			if (retryable) {
				throw failure;
			}
		} catch (ResourceAccessException failure) {
			orderStateService.recordFailure(orderId, step, "HTTP connection or timeout failure", false);
			throw failure;
		} catch (RestClientException | IllegalArgumentException failure) {
			orderStateService.recordFailure(orderId, step, "Invalid external response", true);
		}
	}

	private void perform(AzOrderView customerOrder, AzOrderStep step) {
		String idempotencyKey = step.taskName() + ":" + customerOrder.id();
		String externalId = customerOrder.externalId();
		String response;
		switch (step) {
			case REGISTER_ORDER -> {
				externalId = orderGateway.register(customerOrder, idempotencyKey);
				response = externalId;
			}
			case GET_ORDER -> response = orderGateway.fetch(externalId);
			case SEND_ORDER -> response = orderGateway.send(customerOrder.payload(), idempotencyKey);
			default -> throw new IllegalArgumentException("Unknown step");
		}
		orderStateService.complete(customerOrder.id(), step, externalId, response);
	}
}
