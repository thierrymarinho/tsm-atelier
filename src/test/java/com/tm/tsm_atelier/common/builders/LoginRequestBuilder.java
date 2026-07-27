package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;

public class LoginRequestBuilder {

	private String email = "user@email.com";
	private String password = "senhaForte123";

	public static LoginRequestBuilder aLoginRequest() {
		return new LoginRequestBuilder();
	}

	public LoginRequestBuilder withEmail(String email) {
		this.email = email;
		return this;
	}

	public LoginRequestBuilder withPassword(String password) {
		this.password = password;
		return this;
	}

	public LoginRequestDTO build() {
		return new LoginRequestDTO(email, password);
	}
}
