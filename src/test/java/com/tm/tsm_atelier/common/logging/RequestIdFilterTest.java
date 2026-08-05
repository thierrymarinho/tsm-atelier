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
	@DisplayName("Generates an id when the client sends none and echoes it back in the header")
	void shouldGenerateAnIdWhenTheClientSendsNone() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest(), response, captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).isNotBlank();
		assertThat(response.getHeader(HEADER)).isEqualTo(idDuringChain.get());
	}

	@Test
	@DisplayName("Reuses the id sent by the client so the trail survives across services")
	void shouldReuseTheIdSentByTheClient() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HEADER, "front-42");
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).isEqualTo("front-42");
	}

	@Test
	@DisplayName("Strips line breaks from the header to prevent forged log lines")
	void shouldStripLineBreaksFromTheHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HEADER, "abc\n2026-01-01 ERROR forged log line");
		AtomicReference<String> idDuringChain = new AtomicReference<>();

		filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idDuringChain));

		assertThat(idDuringChain.get()).doesNotContain("\n").doesNotContain(" ");
	}

	@Test
	@DisplayName("Clears the MDC on the way out so the next request cannot inherit the id")
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
