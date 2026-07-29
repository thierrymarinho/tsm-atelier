package com.tm.tsm_atelier.domain.common.port;

public interface EmailPort {

	void sendVerificationEmail(String to, String firstName, String verificationLink);

	void sendOrderConfirmationEmail(String to, String firstName, Long orderId, java.math.BigDecimal totalAmount);
}
