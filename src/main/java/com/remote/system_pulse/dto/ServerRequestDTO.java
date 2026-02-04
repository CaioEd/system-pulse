package com.remote.system_pulse.dto;

import jakarta.validation.constraints.*;


// records are used to transport immutable data
public record ServerRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name, 

    @Size(max = 500, message = "Description must be between 10 and 500 characters")   // same limit as model
    String description
) {}
