package com.tm.tsm_atelier.domain.user.dto;

import com.tm.tsm_atelier.domain.user.enums.State;

public record AddressResponseDTO(Long id, String street, String number, String complement, String neighborhood,
		String city, State state, String postalCode, boolean isDefault) {
}
