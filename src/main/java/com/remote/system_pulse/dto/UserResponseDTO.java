package com.remote.system_pulse.dto;

public record UserResponseDTO(
    Long id,
    String name,
    String email,
    String phoneNumber
) {}
