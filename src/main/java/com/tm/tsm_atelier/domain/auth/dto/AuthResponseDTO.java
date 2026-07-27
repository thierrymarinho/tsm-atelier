package com.tm.tsm_atelier.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponseDTO(@JsonIgnore String accessToken, @JsonIgnore String refreshToken, String email,
		String name) {
}
