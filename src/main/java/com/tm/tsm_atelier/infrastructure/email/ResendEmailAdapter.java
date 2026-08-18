package com.tm.tsm_atelier.infrastructure.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.tm.tsm_atelier.domain.common.port.EmailPort;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailAdapter implements EmailPort {

	private static final Logger logger = LoggerFactory.getLogger(ResendEmailAdapter.class);

	private static final Locale BRAZIL = Locale.of("pt", "BR");

	private final Resend resend;

	private final EmailTemplateRenderer templates;

	@Value("${resend.from-email}")
	private String fromEmail;

	@Value("${app.email.banner-url}")
	private String bannerUrl;

	public ResendEmailAdapter(Resend resend, EmailTemplateRenderer templates) {
		this.resend = resend;
		this.templates = templates;
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendVerificationEmail(String to, String firstName, String verificationLink) {
		Map<String, String> values = Map.of("FIRST_NAME", firstName, "ACTION_URL", verificationLink);

		String html = shell(templates.render("header-banner.html", Map.of("BANNER_URL", bannerUrl)),
				templates.render("verification.html", values),
				"Falta um passo para concluir seu cadastro na TSM Atelier.");

		send(to, "Bem-vindo(a) à TSM Atelier — confirme seu e-mail", html,
				templates.renderPlain("verification.txt", values), "Verification email");
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendAccountAlreadyExistsEmail(String to, String firstName, String loginLink) {
		Map<String, String> values = Map.of("FIRST_NAME", firstName, "ACTION_URL", loginLink);

		String html = shell(templates.renderPlain("header-wordmark.html", Map.of()),
				templates.render("account-exists.html", values), "Você já tem uma conta na TSM Atelier.");

		send(to, "Você já tem uma conta — TSM Atelier", html, templates.renderPlain("account-exists.txt", values),
				"Account-already-exists notice");
	}

	@Override
	@Async("emailTaskExecutor")
	public void sendOrderConfirmationEmail(String to, String firstName, Long orderId, BigDecimal totalAmount) {
		Map<String, String> values = Map.of("FIRST_NAME", firstName, "ORDER_ID", String.valueOf(orderId), "TOTAL",
				formatBrl(totalAmount));

		String html = shell(templates.renderPlain("header-wordmark.html", Map.of()),
				templates.render("order-confirmation.html", values),
				"Recebemos o seu pagamento e já estamos preparando o pedido.");

		send(to, "Confirmação de Pedido #" + orderId + " — TSM Atelier", html,
				templates.renderPlain("order-confirmation.txt", values), "Order confirmation email");
	}

	private String shell(String header, String content, String preheader) {
		return templates.renderPlain("shell.html",
				Map.of("HEADER", header, "CONTENT", content, "PREHEADER", preheader));
	}

	/**
	 * A versão em texto acompanha o HTML em todos os envios: filtro de spam
	 * penaliza mensagem só-HTML, e o domínio remetente aqui é novo — sem reputação
	 * acumulada, essa penalidade é a diferença entre a caixa de entrada e a aba de
	 * promoções.
	 *
	 * O catch é largo de propósito, ao contrário do de RateLimitService. Emails
	 * .send declara throws ResendException, mas em resposta de erro do provedor
	 * lança RuntimeException não declarada — verificado com chave inválida, que
	 * subiu "Failed to send email: 401" direto para o executor. Um catch estreito
	 * aqui perde justamente as falhas mais comuns: chave errada, domínio não
	 * verificado, rate limit do provedor.
	 *
	 * Nada sobe porque isto roda em @Async e ninguém está esperando: deixar escapar
	 * so troca esta mensagem, que diz o destinatário, por um despejo genérico do
	 * handler do executor. A exceção inteira vai para o log, com stack trace, para
	 * que erro de programação continue visível.
	 */
	private void send(String to, String subject, String html, String text, String description) {
		CreateEmailOptions options = CreateEmailOptions.builder().from(fromEmail).to(to).subject(subject).html(html)
				.text(text).build();

		try {
			resend.emails().send(options);
			logger.info("{} sent to {}", description, to);
		} catch (Exception e) {
			logger.error("Failed to send {} to {}", description, to, e);
		}
	}

	/**
	 * DecimalFormat com símbolos pt-BR, e não NumberFormat.getCurrencyInstance: a
	 * instância de moeda separa o "R$" do número com espaço não-quebrável, e o
	 * caractere exato mudou entre versões do JDK. Aqui a saída é sempre "R$
	 * 1.234,50". A instância é criada por chamada porque DecimalFormat não é
	 * thread-safe e estes métodos rodam no executor de e-mail.
	 */
	private static String formatBrl(BigDecimal amount) {
		DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BRAZIL));
		return "R$ " + format.format(amount);
	}
}
