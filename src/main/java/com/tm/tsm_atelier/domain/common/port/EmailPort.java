package com.tm.tsm_atelier.domain.common.port;

public interface EmailPort {

	void sendVerificationEmail(String to, String firstName, String verificationLink);

	/**
	 * Avisa que alguem tentou criar conta com um e-mail que ja tem uma. E a metade
	 * fora de banda da resposta generica do registro: a API responde igual para
	 * e-mail novo e existente para nao permitir enumeracao, e quem de fato controla
	 * a caixa recebe aqui a informacao que a resposta HTTP deixou de dar.
	 */
	void sendAccountAlreadyExistsEmail(String to, String firstName, String loginLink);

	void sendOrderConfirmationEmail(String to, String firstName, Long orderId, java.math.BigDecimal totalAmount);
}
