package com.remote.system_pulse.dto;

import java.time.LocalDateTime;
import com.remote.system_pulse.model.enums.ServerStatus;

public record ServerResponseDTO (
    Long id,
    String name,
    String description,
    String ip,
    Integer port,
    ServerStatus status,
    LocalDateTime lastChecked
){}
