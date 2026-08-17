package com.tm.tsm_atelier.infrastructure.email;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Os templates usam {{TOKEN}} em vez de %s porque HTML de e-mail vive de
 * width="100%", e String.formatted sobre um text block com "100%" lanca
 * UnknownFormatConversionException — o autor teria que escrever 100%% em toda
 * ocorrencia e lembrar disso para sempre.
 *
 * Tudo e lido uma vez no boot, e um arquivo faltando derruba a aplicacao. E de
 * proposito: o envio roda em @Async com o erro apenas logado, entao um template
 * ausente em producao viraria e-mail que nunca chega, sem ninguem perceber.
 * Vale mais falhar no deploy, como ja acontece com JWT_SECRET.
 */
@Component
public class EmailTemplateRenderer {

	private static final String BASE_PATH = "templates/email/";

	private static final List<String> TEMPLATES = List.of("shell.html", "header-banner.html", "header-wordmark.html",
			"verification.html", "account-exists.html", "order-confirmation.html", "verification.txt",
			"account-exists.txt", "order-confirmation.txt");

	private static final Pattern UNFILLED_TOKEN = Pattern.compile("\\{\\{[A-Z_]+\\}\\}");

	private final Map<String, String> templates = new HashMap<>();

	@PostConstruct
	void load() {
		for (String name : TEMPLATES) {
			try (InputStream in = new ClassPathResource(BASE_PATH + name).getInputStream()) {
				templates.put(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException("Email template missing from the classpath: " + BASE_PATH + name, e);
			}
		}
	}

	/**
	 * Escapa os valores. E o metodo para tudo que vem de fora — nome de cliente,
	 * link, valor de pedido.
	 */
	public String render(String template, Map<String, String> values) {
		return substitute(template, values, true);
	}

	/**
	 * Nao escapa. Serve para dois casos e so para eles: montar o shell, cujos
	 * valores sao fragmentos de HTML ja renderizados e validados, e as versoes em
	 * texto puro, onde escapar transformaria "&" em "&amp;" na cara do leitor.
	 */
	public String renderPlain(String template, Map<String, String> values) {
		return substitute(template, values, false);
	}

	private String substitute(String template, Map<String, String> values, boolean escape) {
		String content = templates.get(template);

		if (content == null) {
			throw new IllegalArgumentException("Unknown email template: " + template);
		}

		for (Map.Entry<String, String> value : values.entrySet()) {
			String replacement = escape ? escapeHtml(value.getValue()) : value.getValue();
			content = content.replace("{{" + value.getKey() + "}}", replacement);
		}

		Matcher leftover = UNFILLED_TOKEN.matcher(content);
		if (leftover.find()) {
			throw new IllegalStateException(
					"Token " + leftover.group() + " was left unfilled in email template " + template);
		}

		return content;
	}

	/**
	 * Escrito a mao em vez de HtmlUtils.htmlEscape porque aquele usa ISO-8859-1 por
	 * padrao e converteria todo acento em entidade — "José" viraria "Jos&eacute;"
	 * no fonte. O "&" vem primeiro, senao ele escaparia as entidades geradas pelas
	 * substituicoes seguintes.
	 */
	private static String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
