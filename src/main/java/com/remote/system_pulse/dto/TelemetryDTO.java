package com.remote.system_pulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TelemetryDTO(
    @NotNull(message = "usageCpu is required")
    @Min(0) @Max(100)
    Double usageCpu,

    @NotNull(message = "usageRam is required")
    @Min(0) @Max(100)
    Double usageRam,

    @NotNull(message = "usageDisk is required")
    @Min(0) @Max(100)
    Double usageDisk
) {}
