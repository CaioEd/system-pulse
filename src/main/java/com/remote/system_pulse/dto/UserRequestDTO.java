package com.remote.system_pulse.dto;

import jakarta.validation.constraints.*;

public record UserRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Pattern(regexp = "^\\+?[0-9\\s\\-().]{7,20}$", message = "Phone number is invalid")
    String phoneNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}
