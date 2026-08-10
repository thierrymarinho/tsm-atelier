package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Os atributos do cookie CSRF sao contrato com o browser, e nao detalhe de
 * configuracao: quem decide se ele e enviado, se trafega em claro e se o
 * JavaScript consegue le-lo e o browser, a partir dai.
 *
 * <p>
 * Precisa de servidor real porque o que se verifica e o header Set-Cookie
 * montado na resposta HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CSRF cookie attributes")
class CsrfCookieAttributesTest {

	@LocalServerPort
	private int port;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	@DisplayName("Should emit the CSRF cookie readable by JavaScript, over TLS only, and scoped to this site")
	void shouldEmitCsrfCookieWithTheRightAttributes() throws Exception {
		String setCookie = csrfSetCookieHeader();

		// Legivel pelo JavaScript de proposito: e assim que o front devolve o valor
		// no header X-XSRF-TOKEN. O token nao e credencial — nao autentica ninguem.
		assertThat(setCookie).doesNotContain("HttpOnly");

		// Os cookies de sessao ja exigiam TLS; este trafegava em claro.
		assertThat(setCookie).contains("Secure");

		// Strict, e nao Lax: front e API respondem pela mesma origem por tras do
		// rewrite, entao nao ha requisicao cross-site legitima a preservar.
		assertThat(setCookie).contains("SameSite=Strict");

		assertThat(setCookie).contains("Path=/");
	}

	/**
	 * O prefixo so vale enquanto os tres atributos que o browser exige estiverem
	 * presentes — Secure, Path=/ e nenhum Domain. Se qualquer um cair, o browser
	 * recusa o cookie inteiro e o double-submit para de funcionar em silencio: o
	 * front nao encontra o token, e toda escrita passa a responder 403.
	 *
	 * <p>
	 * A ausencia de Domain e o que da a garantia que importa aqui. Com ela, nenhuma
	 * outra origem consegue sobrescrever este cookie — e um atacante que pudesse
	 * escolher o valor mandaria o header correspondente e passaria pela
	 * verificacao.
	 */
	@Test
	@DisplayName("Should keep the __Host- prefix backed by the attributes the browser demands for it")
	void shouldKeepTheHostPrefixValid() throws Exception {
		String setCookie = csrfSetCookieHeader();

		assertThat(setCookie).startsWith("__Host-");
		assertThat(setCookie).contains("Secure");
		assertThat(setCookie).contains("Path=/");
		assertThat(setCookie).doesNotContain("Domain=");
	}

	private String csrfSetCookieHeader() throws Exception {
		HttpResponse<String> response = client.send(HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + "/api/v1/catalog/products?size=1")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		List<String> cookies = response.headers().allValues("Set-Cookie");

		return cookies.stream().filter(cookie -> cookie.startsWith(SecurityConstants.CSRF_COOKIE + "=")).findFirst()
				.orElseThrow(() -> new AssertionError(
						"nenhum cookie " + SecurityConstants.CSRF_COOKIE + " foi emitido: " + cookies));
	}
}
