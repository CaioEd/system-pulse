package com.remote.system_pulse.dto;

import com.remote.system_pulse.model.enums.ServerStatus;
import java.time.LocalDateTime;

// Objeto JSON enviado ao cliente via canal WS
public record StatusUpdateEvent(
    Long serverId,
    String ip,
    ServerStatus status,
    LocalDateTime timestamp
) {}