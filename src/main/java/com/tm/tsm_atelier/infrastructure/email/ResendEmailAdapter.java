package com.tm.tsm_atelier.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.tm.tsm_atelier.domain.common.port.EmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailAdapter implements EmailPort {

	private static final Logger logger = LoggerFactory.getLogger(ResendEmailAdapter.class);

	private final Resend resend;

	@Value("${resend.from-email}")
	private String fromEmail;

	public ResendEmailAdapter(Resend resend) {
		this.resend = resend;
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendVerificationEmail(String to, String firstName, String verificationLink) {
		String html = buildVerificationEmailHtml(firstName, verificationLink);

		CreateEmailOptions options = CreateEmailOptions.builder().from(fromEmail).to(to)
				.subject("Verify your email — TSM Atelier").html(html).build();

		try {
			resend.emails().send(options);
			logger.info("Verification email sent to {}", to);
		} catch (ResendException e) {
			// Loga o erro sem propagar — a thread async não deve quebrar o fluxo principal
			logger.error("Failed to send verification email to {}: {}", to, e.getMessage());
		}
	}

	private String buildVerificationEmailHtml(String firstName, String verificationLink) {
		return """
				<!DOCTYPE html>
				<html>
				  <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
				    <h2>Bem-vindo(a) à TSM Atelier, %s!</h2>
				    <p>Por favor, verifique seu endereço de e-mail para concluir seu cadastro.</p>
				    <a href="%s"
				       style="display: inline-block; padding: 12px 24px; background-color: #1a1a1a;
				              color: #ffffff; text-decoration: none; border-radius: 4px; margin: 16px 0;">
				      Verificar e-mail
				    </a>
				    <p style="color: #666; font-size: 14px;">Este link de verificação expira em 24 horas.</p>
				    <p style="color: #666; font-size: 14px;">Se você não criou uma conta, pode ignorar este e-mail com segurança.</p>
				  </body>
				</html>
				"""
				.formatted(firstName, verificationLink);
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendAccountAlreadyExistsEmail(String to, String firstName, String loginLink) {
		String html = buildAccountAlreadyExistsEmailHtml(firstName, loginLink);

		CreateEmailOptions options = CreateEmailOptions.builder().from(fromEmail).to(to)
				.subject("Você já tem uma conta — TSM Atelier").html(html).build();

		try {
			resend.emails().send(options);
			logger.info("Account-already-exists notice sent to {}", to);
		} catch (ResendException e) {
			logger.error("Failed to send account-already-exists notice to {}: {}", to, e.getMessage());
		}
	}

	/**
	 * Não diz "alguém tentou se cadastrar como você" de forma alarmista, e também
	 * não esconde o ocorrido: quem recebe ou foi a própria pessoa que esqueceu que
	 * tinha conta — o caso comum — ou precisa saber que o endereço dela foi
	 * digitado em um cadastro.
	 */
	private String buildAccountAlreadyExistsEmailHtml(String firstName, String loginLink) {
		return """
				<!DOCTYPE html>
				<html>
				  <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
				    <h2>Olá, %s!</h2>
				    <p>Recebemos um cadastro na TSM Atelier com este endereço de e-mail, mas você já tem uma conta conosco.</p>
				    <p>Se foi você, é só entrar normalmente:</p>
				    <a href="%s"
				       style="display: inline-block; padding: 12px 24px; background-color: #1a1a1a;
				              color: #ffffff; text-decoration: none; border-radius: 4px; margin: 16px 0;">
				      Entrar na minha conta
				    </a>
				    <p style="color: #666; font-size: 14px;">Se não foi você, pode ignorar este e-mail — nenhuma conta nova foi criada e nada mudou na sua.</p>
				  </body>
				</html>
				"""
				.formatted(firstName, loginLink);
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendOrderConfirmationEmail(String to, String firstName, Long orderId,
			java.math.BigDecimal totalAmount) {
		String html = buildOrderConfirmationEmailHtml(firstName, orderId, totalAmount);

		CreateEmailOptions options = CreateEmailOptions.builder().from(fromEmail).to(to)
				.subject("Confirmação de Pedido #" + orderId + " — TSM Atelier").html(html).build();

		try {
			resend.emails().send(options);
			logger.info("Order confirmation email sent to {}", to);
		} catch (ResendException e) {
			logger.error("Failed to send order confirmation email to {}: {}", to, e.getMessage());
		}
	}

	private String buildOrderConfirmationEmailHtml(String firstName, Long orderId, java.math.BigDecimal totalAmount) {
		return """
				<!DOCTYPE html>
				<html>
				  <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
				    <h2>Olá, %s! Seu pedido foi confirmado.</h2>
				    <p>Obrigado por comprar na TSM Atelier. Recebemos o seu pagamento e já estamos preparando o seu pedido.</p>
				    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 4px; margin: 20px 0;">
				      <p><strong>Pedido:</strong> #%d</p>
				      <p><strong>Total:</strong> R$ %s</p>
				    </div>
				    <p>Você será notificado assim que o seu pacote for enviado.</p>
				  </body>
				</html>
				"""
				.formatted(firstName, orderId, totalAmount.toString());
	}
}
