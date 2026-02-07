package com.remote.system_pulse.dto;

import java.time.LocalDateTime;
import com.remote.system_pulse.model.enums.ServerStatus;
import java.util.UUID;

public record ServerResponseDTO (
    Long id,
    UUID token,
    String name,
    String description,
    String ip,
    ServerStatus status,
    LocalDateTime lastHeartbeat,
    Double usageCpu,
    Double usageRam,
    Double usageDisk
){}
