package com.example.extension.az.service;

import com.example.extension.az.model.dto.AzOrderCommand;
import com.example.extension.az.model.dto.AzOrderView.TaskView;
import com.example.extension.az.model.dto.AzOrderView;
import com.example.extension.az.model.entity.AzOrder;
import com.example.extension.az.model.entity.AzTaskResult;
import com.example.extension.az.repository.AzOrderRepository;
import com.example.extension.az.repository.AzTaskResultRepository;
import com.example.extension.az.tasks.AzOrderStep;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class AzOrderStateService {
	private final AzOrderRepository orderRepository;
	private final AzTaskResultRepository taskResultRepository;
	private final SchedulerClient schedulerClient;

	@Transactional
	public AzOrderView create(AzOrderCommand command) {
		AzOrder customerOrder = orderRepository.save(new AzOrder(command.details()));
		schedule(customerOrder.getId(), AzOrderStep.REGISTER_ORDER);
		return view(customerOrder);
	}

	@Transactional(readOnly = true)
	public AzOrderView get(UUID orderId) {
		return view(orderRepository.findById(orderId).orElseThrow(this::notFound));
	}

	@Transactional
	public void complete(UUID orderId, AzOrderStep step, String externalId, String response) {
		AzOrder customerOrder = orderRepository.findForUpdate(orderId).orElseThrow(this::notFound);
		if (!customerOrder.getStatus().equals(step.expectedStatus())) {
			return;
		}
		String payload = step == AzOrderStep.GET_ORDER ? response : customerOrder.getPayload();
		customerOrder.complete(step.completedStatus(), externalId, payload);
		taskResultRepository.save(new AzTaskResult(
				orderId, step.taskName(), "SUCCESS", response, null));
		if (step.next() != null) {
			schedule(orderId, step.next());
		}
	}

	@Transactional
	public void recordFailure(UUID orderId, AzOrderStep step, String error, boolean terminal) {
		AzOrder customerOrder = orderRepository.findForUpdate(orderId).orElseThrow(this::notFound);
		if (!customerOrder.getStatus().equals(step.expectedStatus())) {
			return;
		}
		taskResultRepository.save(new AzTaskResult(
				orderId, step.taskName(), "FAILURE", null, error));
		if (terminal) {
			customerOrder.fail();
		}
	}

	@Transactional
	public void exhaustRetries(UUID orderId, AzOrderStep step) {
		AzOrder customerOrder = orderRepository.findForUpdate(orderId).orElseThrow(this::notFound);
		if (customerOrder.getStatus().equals(step.expectedStatus())) {
			customerOrder.fail();
		}
	}

	private void schedule(UUID orderId, AzOrderStep step) {
		schedulerClient.scheduleIfNotExists(
				step.descriptor().instance(orderId.toString()).scheduledTo(Instant.now()));
	}

	private AzOrderView view(AzOrder customerOrder) {
		var history = taskResultRepository.findByOrderIdOrderById(customerOrder.getId()).stream()
				.map(result -> new TaskView(result.getTaskName(), result.getOutcome(),
						result.getResponse(), result.getError(), result.getPerformedAt()))
				.toList();
		return new AzOrderView(customerOrder.getId(), customerOrder.getDescription(),
				customerOrder.getStatus(), customerOrder.getExternalId(), customerOrder.getPayload(),
				customerOrder.getCreatedAt(), customerOrder.getUpdatedAt(), history);
	}

	private NoSuchElementException notFound() {
		return new NoSuchElementException("Order not found");
	}
}
