package com.tm.tsm_atelier.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.tm.tsm_atelier.infrastructure.email.EmailTemplateRenderer;
import com.tm.tsm_atelier.infrastructure.email.ResendEmailAdapter;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResendEmailAdapterTest {

	private static final String BANNER = "https://cdn.test/capa-email.jpg";

	@Mock
	private Resend resend;

	@Mock
	private Emails emails;

	private ResendEmailAdapter adapter;

	@BeforeEach
	void setUp() {
		EmailTemplateRenderer renderer = new EmailTemplateRenderer();
		ReflectionTestUtils.invokeMethod(renderer, "load");

		adapter = new ResendEmailAdapter(resend, renderer);
		ReflectionTestUtils.setField(adapter, "fromEmail", "noreply@tsm-atelier.com");
		ReflectionTestUtils.setField(adapter, "bannerUrl", BANNER);
	}

	private CreateEmailOptions captureSent() throws ResendException {
		ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
		verify(emails).send(captor.capture());
		return captor.getValue();
	}

	private void resendAccepts() throws ResendException {
		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class))).thenReturn(new CreateEmailResponse());
	}

	@Test
	@DisplayName("Should send the verification email with the correct fields")
	void shouldSendVerificationEmailWithCorrectFields() throws ResendException {
		String to = "user@example.com";
		String firstName = "Maria";
		String link = "http://localhost:3000/verify-email?token=abc123";
		resendAccepts();

		adapter.sendVerificationEmail(to, firstName, link);

		CreateEmailOptions sent = captureSent();
		assertThat(sent.getFrom()).isEqualTo("noreply@tsm-atelier.com");
		assertThat(sent.getTo()).contains(to);
		assertThat(sent.getSubject()).isEqualTo("Bem-vindo(a) à TSM Atelier — confirme seu e-mail");
		assertThat(sent.getHtml()).contains(firstName).contains(link).contains(BANNER);
	}

	/**
	 * A imagem carrega a marca e o "Bem-vindo" em pixels, e Gmail e Outlook
	 * bloqueiam imagem de remetente sem reputação. Sem o link em texto, quem abre
	 * com imagens desligadas e sem HTML não tem como verificar a conta.
	 */
	@Test
	@DisplayName("Should also send a plain text version carrying the same link")
	void shouldSendPlainTextAlternative() throws ResendException {
		String link = "http://localhost:3000/verify-email?token=abc123";
		resendAccepts();

		adapter.sendVerificationEmail("user@example.com", "Maria", link);

		CreateEmailOptions sent = captureSent();
		assertThat(sent.getText()).isNotBlank().contains("Maria").contains(link).doesNotContain("<td");
	}

	@Test
	@DisplayName("Should escape the customer name in HTML and keep it literal in plain text")
	void shouldEscapeNameInHtmlOnly() throws ResendException {
		resendAccepts();

		adapter.sendVerificationEmail("user@example.com", "Ana <b>&</b>", "http://localhost:3000/v?t=1");

		CreateEmailOptions sent = captureSent();
		assertThat(sent.getHtml()).contains("Ana &lt;b&gt;&amp;&lt;/b&gt;").doesNotContain("Ana <b>&");
		assertThat(sent.getText()).contains("Ana <b>&</b>");
	}

	/**
	 * O banner traz "BEM-VINDO" gravado na imagem — usá-lo aqui diria a coisa
	 * errada para quem só tentou se cadastrar de novo.
	 */
	@Test
	@DisplayName("Should not use the welcome banner on the account-already-exists notice")
	void shouldUseWordmarkHeaderOnAccountExistsNotice() throws ResendException {
		resendAccepts();

		adapter.sendAccountAlreadyExistsEmail("user@example.com", "Maria", "http://localhost:3000/login");

		CreateEmailOptions sent = captureSent();
		assertThat(sent.getSubject()).isEqualTo("Você já tem uma conta — TSM Atelier");
		assertThat(sent.getHtml()).doesNotContain(BANNER).contains("TSM&nbsp;ATELIER")
				.contains("http://localhost:3000/login");
	}

	@Test
	@DisplayName("Should format the order total as Brazilian currency")
	void shouldFormatOrderTotalAsBrl() throws ResendException {
		resendAccepts();

		adapter.sendOrderConfirmationEmail("user@example.com", "Maria", 42L, new BigDecimal("1234.5"));

		CreateEmailOptions sent = captureSent();
		assertThat(sent.getSubject()).isEqualTo("Confirmação de Pedido #42 — TSM Atelier");
		assertThat(sent.getHtml()).contains("R$ 1.234,50").contains("#42");
		assertThat(sent.getText()).contains("R$ 1.234,50");
	}

	@Test
	@DisplayName("Should log the error without propagating when sending fails")
	void shouldNotPropagateExceptionWhenEmailFails() throws ResendException {
		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class))).thenThrow(new ResendException("API error"));

		adapter.sendVerificationEmail("user@example.com", "Maria", "http://localhost:3000/verify?token=abc");

		verify(emails).send(any(CreateEmailOptions.class));
	}

	/**
	 * Emails.send declara throws ResendException, mas em resposta de erro do
	 * provedor lança RuntimeException não declarada. Observado ao disparar o fluxo
	 * real com chave inválida: "Failed to send email: 401" escapou do catch e foi
	 * parar no handler do executor async. Este teste fixa o caso que de fato
	 * acontece em produção — chave errada, domínio não verificado, rate limit.
	 */
	@Test
	@DisplayName("Should also swallow the undeclared RuntimeException the SDK throws on HTTP errors")
	void shouldNotPropagateUndeclaredRuntimeException() throws ResendException {
		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class)))
				.thenThrow(new RuntimeException("Failed to send email: 401 {\"message\":\"API key is invalid\"}"));

		adapter.sendVerificationEmail("user@example.com", "Maria", "http://localhost:3000/verify?token=abc");

		verify(emails).send(any(CreateEmailOptions.class));
	}
}
