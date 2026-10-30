package com.example.extension.az.model.dto;

public record AzOrderCommand(String details) {
	public AzOrderCommand {
		if (details == null || details.isBlank() || details.length() > 200) {
			throw new IllegalArgumentException("details must contain 1 to 200 nonblank characters");
		}
	}
}
