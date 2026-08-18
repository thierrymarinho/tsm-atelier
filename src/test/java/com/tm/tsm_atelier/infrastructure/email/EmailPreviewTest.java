package com.tm.tsm_atelier.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Grava em build/email-preview/ o HTML exato que seria enviado, para abrir no
 * navegador. Não substitui teste em cliente real — Outlook e Gmail renderizam
 * diferente do Chrome —, mas evita gastar um envio de verdade a cada ajuste de
 * espaçamento.
 *
 * Passa pelo adapter em vez de montar os templates por conta própria: é o que
 * garante que o arquivo aberto seja o e-mail, e não uma aproximação dele.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pré-visualização dos e-mails")
class EmailPreviewTest {

	private static final Path OUTPUT = Path.of("build", "email-preview");

	@Mock
	private Resend resend;

	@Mock
	private Emails emails;

	private ResendEmailAdapter adapter;

	@BeforeEach
	void setUp() {
		EmailTemplateRenderer renderer = new EmailTemplateRenderer();
		renderer.load();

		adapter = new ResendEmailAdapter(resend, renderer);
		ReflectionTestUtils.setField(adapter, "fromEmail", "atelier@tsm-atelier.com");
		ReflectionTestUtils.setField(adapter, "bannerUrl",
				"https://res.cloudinary.com/apgaq55g/image/upload/w_1200,q_auto,f_auto/v1787000415/capa-email_ldtmgi.jpg");
	}

	@Test
	@DisplayName("grava os três e-mails renderizados em build/email-preview")
	void writesPreviews() throws ResendException, IOException {
		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class))).thenReturn(new CreateEmailResponse());

		adapter.sendVerificationEmail("cliente@example.com", "Thierry", "http://localhost:3000/verificar?token=preview");
		adapter.sendAccountAlreadyExistsEmail("cliente@example.com", "Thierry", "http://localhost:3000/login");
		adapter.sendOrderConfirmationEmail("cliente@example.com", "Thierry", 1042L, new BigDecimal("3890.00"));

		ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
		verify(emails, org.mockito.Mockito.times(3)).send(captor.capture());

		List<String> names = List.of("verification", "account-exists", "order-confirmation");
		List<CreateEmailOptions> sent = captor.getAllValues();

		Files.createDirectories(OUTPUT);

		for (int i = 0; i < names.size(); i++) {
			Path html = OUTPUT.resolve(names.get(i) + ".html");
			Files.writeString(html, sent.get(i).getHtml(), StandardCharsets.UTF_8);
			Files.writeString(OUTPUT.resolve(names.get(i) + ".txt"), sent.get(i).getText(), StandardCharsets.UTF_8);

			assertThat(html).exists();
			assertThat(Files.readString(html)).contains("Thierry").doesNotContain("{{");
		}
	}
}
