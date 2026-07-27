package com.tm.tsm_atelier.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
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

	@Mock
	private Resend resend;

	@Mock
	private Emails emails;

	private ResendEmailAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new ResendEmailAdapter(resend);
		ReflectionTestUtils.setField(adapter, "fromEmail", "noreply@tsm-atelier.com");
	}

	@Test
	@DisplayName("Deve enviar email de verificação com os campos corretos")
	void shouldSendVerificationEmailWithCorrectFields() throws ResendException {
		// Arrange
		String to = "user@example.com";
		String firstName = "Maria";
		String link = "http://localhost:3000/verify-email?token=abc123";

		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class))).thenReturn(new CreateEmailResponse());

		// Act
		adapter.sendVerificationEmail(to, firstName, link);

		// Assert — captura o objeto enviado e verifica os campos
		ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
		verify(emails).send(captor.capture());

		CreateEmailOptions sent = captor.getValue();
		assertThat(sent.getFrom()).isEqualTo("noreply@tsm-atelier.com");
		assertThat(sent.getTo()).contains(to);
		assertThat(sent.getSubject()).isEqualTo("Verify your email — TSM Atelier");
		assertThat(sent.getHtml()).contains(firstName);
		assertThat(sent.getHtml()).contains(link);
	}

	@Test
	@DisplayName("Deve logar o erro sem propagar exceção quando o envio falha")
	void shouldNotPropagateExceptionWhenEmailFails() throws ResendException {
		// Arrange
		when(resend.emails()).thenReturn(emails);
		when(emails.send(any(CreateEmailOptions.class))).thenThrow(new ResendException("API error"));

		// Act & Assert — não deve lançar exceção, apenas logar
		adapter.sendVerificationEmail("user@example.com", "Maria", "http://localhost:3000/verify?token=abc");

		verify(emails).send(any(CreateEmailOptions.class));
	}
}
