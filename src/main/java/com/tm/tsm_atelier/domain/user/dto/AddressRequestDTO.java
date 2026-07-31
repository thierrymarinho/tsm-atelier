package com.tm.tsm_atelier.domain.user.dto;

import com.tm.tsm_atelier.domain.user.enums.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequestDTO(
		@NotBlank(message = "Street is required") @Size(max = 255, message = "Street cannot exceed 255 characters") String street,
		@NotBlank(message = "Number is required") @Size(max = 10, message = "Number cannot exceed 10 characters") String number,
		@Size(max = 255, message = "Complement cannot exceed 255 characters") String complement,
		@NotBlank(message = "Neighborhood is required") @Size(max = 100, message = "Neighborhood cannot exceed 100 characters") String neighborhood,
		@NotBlank(message = "City is required") @Size(max = 100, message = "City cannot exceed 100 characters") String city,
		@NotNull(message = "State is required") State state,
		@NotBlank(message = "Postal code is required") @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "Invalid postal code format") String postalCode,
		boolean isDefault) {
}
