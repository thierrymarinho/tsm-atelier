package com.tm.tsm_atelier.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequestDTO(@NotBlank(message = "Token is required") String token) {
}
