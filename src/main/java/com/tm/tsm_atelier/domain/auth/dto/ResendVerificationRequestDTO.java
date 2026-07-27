package com.tm.tsm_atelier.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequestDTO(
		@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
