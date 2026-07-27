package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;

public class AuthResponseDTOBuilder {

	private String accessToken = "mock-access-token";
	private String refreshToken = "mock-refresh-token";
	private String email = "user@example.com";
	private String name = "Maria Silva";

	private AuthResponseDTOBuilder() {
	}

	public static AuthResponseDTOBuilder anAuthResponseDTO() {
		return new AuthResponseDTOBuilder();
	}

	public AuthResponseDTOBuilder withAccessToken(String accessToken) {
		this.accessToken = accessToken;
		return this;
	}

	public AuthResponseDTOBuilder withRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
		return this;
	}

	public AuthResponseDTOBuilder withEmail(String email) {
		this.email = email;
		return this;
	}

	public AuthResponseDTOBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public AuthResponseDTO build() {
		return new AuthResponseDTO(accessToken, refreshToken, email, name);
	}
}
