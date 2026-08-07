package com.tm.tsm_atelier.domain.product.dto;

import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Quando a validacao aceita mais do que a coluna comporta, o erro chega ao
 * cliente como 409 "A data conflict occurred. The resource may already exist."
 * — mensagem de duplicidade para um problema de tamanho. Estes testes prendem
 * os limites da aplicacao aos do banco, para que uma mudanca de coluna sem a
 * mudanca correspondente aqui apareca como teste vermelho.
 */
@DisplayName("Request limits must match the database columns")
class RequestLimitsMatchDatabaseTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	@DisplayName("Should reject an email longer than the users.email column")
	void shouldRejectEmailLongerThanTheColumn() {
		// users.email e VARCHAR(255). @Email valida formato, nao tamanho.
		String longEmail = "a".repeat(250) + "@example.com";

		assertThat(messagesFor(register(longEmail))).contains("Email cannot exceed 255 characters");
	}

	@Test
	@DisplayName("Should accept an email that fits the column")
	void shouldAcceptEmailThatFitsTheColumn() {
		assertThat(validator.validate(register("thierry@example.com"))).isEmpty();
	}

	@Test
	@DisplayName("Should reject a price with more decimal places than the column stores")
	void shouldRejectPriceWithTooManyDecimals() {
		// products.price e DECIMAL(10, 2). Sem @Digits, 29.999 era gravado como
		// 30.00 sem aviso nenhum — arredondamento silencioso num campo de dinheiro.
		ProductRequestDTO request = aProductRequest().withPrice(new BigDecimal("29.999")).build();

		assertThat(messagesFor(request)).anyMatch(message -> message.contains("2 decimal places"));
	}

	@Test
	@DisplayName("Should reject a price with more integer digits than the column stores")
	void shouldRejectPriceAboveTheColumnCapacity() {
		// DECIMAL(10, 2) comporta ate 99.999.999,99.
		ProductRequestDTO request = aProductRequest().withPrice(new BigDecimal("100000000.00")).build();

		assertThat(messagesFor(request)).anyMatch(message -> message.contains("8 integer digits"));
	}

	@Test
	@DisplayName("Should apply the same limit to the promotional price")
	void shouldRejectPromotionalPriceWithTooManyDecimals() {
		ProductRequestDTO request = aProductRequest().withPromotionalPrice(new BigDecimal("19.999")).build();

		assertThat(messagesFor(request)).anyMatch(message -> message.contains("Promotional price"));
	}

	@Test
	@DisplayName("Should accept a price the column stores exactly")
	void shouldAcceptPriceThatFitsTheColumn() {
		ProductRequestDTO request = aProductRequest().withPrice(new BigDecimal("99999999.99"))
				.withPromotionalPrice(new BigDecimal("19.90")).build();

		assertThat(validator.validate(request)).isEmpty();
	}

	private RegisterRequestDTO register(String email) {
		return new RegisterRequestDTO("Thierry", "Marinho", email, "a-valid-password");
	}

	private <T> Set<String> messagesFor(T target) {
		return validator.validate(target).stream().map(ConstraintViolation::getMessage)
				.collect(java.util.stream.Collectors.toSet());
	}
}
