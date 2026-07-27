package com.tm.tsm_atelier.domain.user.dto;

public record AddressResponseDTO(Long id, String street, String number, String complement, String neighborhood,
		String city, String state, String zipCode, boolean isDefault) {
}
