package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import java.util.UUID;

public class UserBuilder {

	private UUID id = UUID.randomUUID();
	private String firstName = "Thierry";
	private String lastName = "Marinho";
	private String email = "thierry@email.com";
	private String password = "$2a$10$dXJ3SW6G7P50lGmMQgel6uMxkD2MKD2sOPeOR9H7hNOpQ1iAO0dWi"; // "senha123456" encoded
	private Role role = Role.CUSTOMER;
	private boolean emailVerified = true; // padrão: verificado, para não quebrar testes existentes

	public static UserBuilder aUser() {
		return new UserBuilder();
	}

	public static UserBuilder anAdmin() {
		return new UserBuilder().withRole(Role.ADMIN).withEmail("admin@tsm-atelier.com");
	}

	public UserBuilder withId(UUID id) {
		this.id = id;
		return this;
	}

	public UserBuilder withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public UserBuilder withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public UserBuilder withEmail(String email) {
		this.email = email;
		return this;
	}

	public UserBuilder withPassword(String password) {
		this.password = password;
		return this;
	}

	public UserBuilder withRole(Role role) {
		this.role = role;
		return this;
	}

	public UserBuilder withEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
		return this;
	}

	public User build() {
		return User.builder().id(id).firstName(firstName).lastName(lastName).email(email).password(password).role(role)
				.emailVerified(emailVerified).build();
	}
}
