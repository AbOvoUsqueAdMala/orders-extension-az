package com.example.extension.az.tasks;

import com.example.extension.az.service.AzOrderStateService;
import com.example.extension.az.service.AzOrderWorkflow;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AzOrderTasks {
	@Bean
	Task<Void> azRegisterOrderTask(ObjectProvider<AzOrderWorkflow> workflow,
			ObjectProvider<AzOrderStateService> state,
			@Value("${orders.retry-delay:2s}") Duration retryDelay,
			@Value("${orders.max-retries:5}") int maxRetries) {
		return task(AzOrderStep.REGISTER_ORDER, workflow, state, retryDelay, maxRetries);
	}

	@Bean
	Task<Void> azGetOrderTask(ObjectProvider<AzOrderWorkflow> workflow,
			ObjectProvider<AzOrderStateService> state,
			@Value("${orders.retry-delay:2s}") Duration retryDelay,
			@Value("${orders.max-retries:5}") int maxRetries) {
		return task(AzOrderStep.GET_ORDER, workflow, state, retryDelay, maxRetries);
	}

	@Bean
	Task<Void> azSendOrderTask(ObjectProvider<AzOrderWorkflow> workflow,
			ObjectProvider<AzOrderStateService> state,
			@Value("${orders.retry-delay:2s}") Duration retryDelay,
			@Value("${orders.max-retries:5}") int maxRetries) {
		return task(AzOrderStep.SEND_ORDER, workflow, state, retryDelay, maxRetries);
	}

	private Task<Void> task(AzOrderStep step, ObjectProvider<AzOrderWorkflow> workflow,
			ObjectProvider<AzOrderStateService> state, Duration retryDelay, int maxRetries) {
		return Tasks.oneTime(step.descriptor())
				.onFailure(FailureHandler.<Void>maxRetries(maxRetries)
						.withBackoff(retryDelay, 2.0)
						.then((completion, operations) -> {
							state.getObject().exhaustRetries(UUID.fromString(
									completion.getExecution().taskInstance.getId()), step);
							operations.remove();
						}))
				.execute((instance, context) ->
						workflow.getObject().execute(UUID.fromString(instance.getId()), step));
	}
}
