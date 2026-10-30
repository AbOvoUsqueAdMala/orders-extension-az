package com.example.extension.az.controller;

import com.example.extension.az.model.dto.AzOrderRequest;
import com.example.extension.az.model.dto.AzOrderView;
import com.example.extension.az.service.AzOrderStateService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extension/az/orders")
@RequiredArgsConstructor
public class AzOrderController {
	private final AzOrderStateService orderStateService;

	@PostMapping
	public ResponseEntity<AzOrderView> create(
			@Valid @RequestBody AzOrderRequest request) {
		var customerOrder = orderStateService.create(request.description());
		return ResponseEntity.accepted()
				.location(URI.create("/api/extension/az/orders/" + customerOrder.id())).body(customerOrder);
	}

	@GetMapping("/{id}")
	public AzOrderView get(@PathVariable UUID id) {
		return orderStateService.get(id);
	}
}
