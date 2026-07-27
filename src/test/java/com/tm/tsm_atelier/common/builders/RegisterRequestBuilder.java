package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;

public class RegisterRequestBuilder {

	private String firstName = "Usuário";
	private String lastName = "Teste";
	private String email = "user@email.com";
	private String password = "senhaForte123";

	public static RegisterRequestBuilder aRegisterRequest() {
		return new RegisterRequestBuilder();
	}

	public RegisterRequestBuilder withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public RegisterRequestBuilder withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public RegisterRequestBuilder withEmail(String email) {
		this.email = email;
		return this;
	}

	public RegisterRequestBuilder withPassword(String password) {
		this.password = password;
		return this;
	}

	public RegisterRequestDTO build() {
		return new RegisterRequestDTO(firstName, lastName, email, password);
	}
}
