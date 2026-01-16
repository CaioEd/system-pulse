package com.remote.system_pulse.dto;

import jakarta.validation.constraints.*;
import com.remote.system_pulse.utils.IpRegex;


// records are used to transport immutable data
public record ServerRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name, 

    @Size(max = 500, message = "Description must be between 10 and 500 characters")   // same limit as model
    String description,

    @NotBlank(message = "IP is required")
    @Pattern(regexp = IpRegex.IP_PATTERN,
             message = "IP inválido (IPv4 ou IPv6)")
    String ip,

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65535")
    Integer port
) {}
