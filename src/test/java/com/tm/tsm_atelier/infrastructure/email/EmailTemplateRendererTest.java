package com.tm.tsm_atelier.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailTemplateRenderer")
class EmailTemplateRendererTest {

	private EmailTemplateRenderer renderer;

	@BeforeEach
	void setUp() {
		renderer = new EmailTemplateRenderer();
		renderer.load();
	}

	@Test
	@DisplayName("escapa o que vem de fora antes de colocar no HTML")
	void escapesExternalValues() {
		String html = renderer.render("verification.html",
				Map.of("FIRST_NAME", "Ana <b>&</b>", "ACTION_URL", "https://x.test/v?t=1"));

		assertThat(html).contains("Ana &lt;b&gt;&amp;&lt;/b&gt;").doesNotContain("<b>&");
	}

	@Test
	@DisplayName("não escapa a versão em texto puro, onde entidade seria lida como lixo")
	void keepsPlainTextLiteral() {
		String text = renderer.renderPlain("verification.txt",
				Map.of("FIRST_NAME", "Ana & Bia", "ACTION_URL", "https://x.test/v?t=1&r=2"));

		assertThat(text).contains("Ana & Bia").contains("https://x.test/v?t=1&r=2").doesNotContain("&amp;");
	}

	/**
	 * O motivo de existir: token esquecido em um template renderiza "{{TOTAL}}"
	 * literal no e-mail do cliente, e nada mais no sistema reclama disso.
	 */
	@Test
	@DisplayName("recusa renderizar com token não preenchido")
	void rejectsUnfilledToken() {
		assertThatThrownBy(() -> renderer.render("verification.html", Map.of("FIRST_NAME", "Ana")))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("{{ACTION_URL}}")
				.hasMessageContaining("verification.html");
	}

	@Test
	@DisplayName("recusa um template que não existe")
	void rejectsUnknownTemplate() {
		assertThatThrownBy(() -> renderer.render("nao-existe.html", Map.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("os nove templates declarados carregam no boot")
	void loadsEveryDeclaredTemplate() {
		assertThat(renderer.renderPlain("header-wordmark.html", Map.of())).contains("TSM");
		assertThat(renderer.render("header-banner.html", Map.of("BANNER_URL", "https://cdn.test/capa.jpg")))
				.contains("https://cdn.test/capa.jpg");
		assertThat(renderer.render("account-exists.html", Map.of("FIRST_NAME", "Ana", "ACTION_URL", "https://x.test")))
				.contains("Ana");
		assertThat(renderer.render("order-confirmation.html",
				Map.of("FIRST_NAME", "Ana", "ORDER_ID", "7", "TOTAL", "R$ 10,00"))).contains("#7");
		assertThat(renderer.renderPlain("account-exists.txt", Map.of("FIRST_NAME", "Ana", "ACTION_URL", "https://x")))
				.isNotBlank();
		assertThat(renderer.renderPlain("order-confirmation.txt",
				Map.of("FIRST_NAME", "Ana", "ORDER_ID", "7", "TOTAL", "R$ 10,00"))).isNotBlank();
		assertThat(renderer.renderPlain("shell.html",
				Map.of("HEADER", "<tr><td>h</td></tr>", "CONTENT", "<tr><td>c</td></tr>", "PREHEADER", "p")))
				.contains("<!DOCTYPE").contains("<tr><td>c</td></tr>");
	}
}
