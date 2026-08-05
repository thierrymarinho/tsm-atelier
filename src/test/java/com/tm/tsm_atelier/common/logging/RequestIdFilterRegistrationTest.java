package com.tm.tsm_atelier.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O RequestIdFilterTest cobre a lógica do filtro isoladamente; este aqui cobre
 * o que ele não alcança: que o filtro realmente entrou na cadeia de servlet. Se
 * a auto-registração deixasse de acontecer, nada quebraria — o campo de
 * requestId simplesmente ficaria vazio em todas as linhas de log, sem nenhum
 * sinal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RequestIdFilter in the application filter chain")
class RequestIdFilterRegistrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Every response carries the request id, even when the route does not exist")
	void everyResponseCarriesTheRequestId() throws Exception {
		// Rota inexistente de propósito: o filtro roda antes do roteamento e da
		// segurança, então o rastro precisa existir independente do desfecho.
		mockMvc.perform(get("/definitely-not-a-route"))
				.andExpect(result -> assertThat(result.getResponse().getHeader("X-Request-Id")).isNotBlank());
	}
}
