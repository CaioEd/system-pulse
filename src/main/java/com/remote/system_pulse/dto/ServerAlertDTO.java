package com.remote.system_pulse.dto;

import com.remote.system_pulse.model.enums.ServerStatus;
import java.time.LocalDateTime;


public record ServerAlertDTO (
    Long id,
    String name,
    String ip,
    ServerStatus status,
    Double usageCpu,
    String alertMessage,
    LocalDateTime alertTime
) {}
