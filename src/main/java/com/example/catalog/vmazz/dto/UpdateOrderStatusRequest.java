package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull(message = "Status is required") OrderStatus status) { }
