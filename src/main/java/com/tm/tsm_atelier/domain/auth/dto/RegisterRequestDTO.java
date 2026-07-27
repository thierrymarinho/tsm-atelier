package com.tm.tsm_atelier.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
		@NotBlank(message = "First name is required") @Size(min = 3, max = 50, message = "First name must be between 3 and 50 characters") String firstName,
		@NotBlank(message = "Last name is required") @Size(min = 3, max = 50, message = "Last name must be between 3 and 50 characters") String lastName,
		@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
		@NotBlank(message = "Password is required") @Size(min = 6, max = 30, message = "Password must be between 6 and 30 characters") String password) {
}
