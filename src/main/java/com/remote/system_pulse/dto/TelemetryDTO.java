package com.remote.system_pulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TelemetryDTO(
    @NotNull(message = "O Token é obrigatório")
    UUID token,
    @Min(0) @Max(100)
    Double usageCpu,

    @Min(0) @Max(100)
    Double usageRam,
    
    @Min(0) @Max(100)
    Double usageDisk
) {}
