package com.tm.tsm_atelier.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RequestIdFilter")
class RequestIdFilterTest {

	private static final String HEADER = "X-Request-Id";

	private final RequestIdFilter filter = new RequestIdFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	@DisplayName("Gera um id quando o cliente não envia nenhum e devolve no header")
	void shouldGenerateAnIdWhenTheClientSendsNone() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest(), response, captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).isNotBlank();
		assertThat(response.getHeader(HEADER)).isEqualTo(idDuringChain.get());
	}

	@Test
	@DisplayName("Reaproveita o id enviado pelo cliente para manter o rastro entre serviços")
	void shouldReuseTheIdSentByTheClient() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HEADER, "front-42");
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).isEqualTo("front-42");
	}

	@Test
	@DisplayName("Remove quebras de linha do header para impedir forja de linhas de log")
	void shouldStripLineBreaksFromTheHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HEADER, "abc\n2026-01-01 ERROR forged log line");
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).doesNotContain("\n").doesNotContain(" ");
	}

	@Test
	@DisplayName("Limpa o MDC no fim para a próxima requisição não herdar o id")
	void shouldClearTheMdcWhenTheRequestEnds() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				captureMdc(new AtomicReference<>()));

		assertThat(MDC.get(RequestIdFilter.REQUEST_ID)).isNull();
	}

	/**
	 * A verificação precisa acontecer dentro da cadeia: depois que o filtro
	 * retorna, o MDC já foi limpo de propósito.
	 */
	private FilterChain captureMdc(AtomicReference<String> target) {
		return (req, res) -> target.set(MDC.get(RequestIdFilter.REQUEST_ID));
	}
}
