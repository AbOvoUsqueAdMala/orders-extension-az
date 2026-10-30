package com.example.extension.az.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AzOrderRequest(@NotBlank @Size(max = 200) String description) {}
