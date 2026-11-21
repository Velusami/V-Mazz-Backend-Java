package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.RecordStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "Status is required") RecordStatus status) { }
