package com.tm.tsm_atelier.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequestDTO(@NotBlank(message = "Street is required") String street,
		@NotBlank(message = "Number is required") String number, String complement,
		@NotBlank(message = "Neighborhood is required") String neighborhood,
		@NotBlank(message = "City is required") String city, @NotBlank(message = "State is required") String state,
		@NotBlank(message = "Zip code is required") String zipCode, boolean isDefault) {
}
