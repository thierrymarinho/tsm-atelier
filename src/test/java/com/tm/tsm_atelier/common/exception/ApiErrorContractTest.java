package com.tm.tsm_atelier.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.user.dto.AddressRequestDTO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

/**
 * O envelope de erro prometido pela documentação, medido no container real.
 *
 * Os três casos aqui foram encontrados exercitando a API de ponta a ponta, e
 * nenhum deles aparecia na suíte: os testes de unidade chamam os handlers já
 * com a exceção montada, e MockMvc não reproduz nem o binder de query string
 * nem a cadeia de filtros que produz o 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Contrato de erro da API")
class ApiErrorContractTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper objectMapper;

	private final HttpClient client = HttpClient.newHttpClient();

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest.Builder to(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	@Test
	@DisplayName("Enum inválido na query string responde como o do corpo")
	void invalidEnumInQueryStringMatchesTheBodyContract() throws Exception {
		HttpResponse<String> response = send(to("/api/v1/catalog/products?category=BANANA").GET().build());

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"field\":\"category\"").contains("\"allowedValues\"")
				.contains("DRESSES");
		assertThat(response.body()).as("o nome do pacote não pode vazar para o cliente")
				.doesNotContain("com.tm.tsm_atelier");
	}

	@Test
	@DisplayName("Booleano ausente no corpo não derruba a desserialização")
	void missingPrimitiveDoesNotBreakDeserialization() {
		AddressRequestDTO parsed = objectMapper.readValue("{\"street\":\"x\"}", AddressRequestDTO.class);

		assertThat(parsed.street()).isEqualTo("x");
		assertThat(parsed.isDefault()).as("ausente vira o default do Java, não um erro").isFalse();
	}

	@Test
	@DisplayName("Booleano nulo explícito também é aceito")
	void explicitNullPrimitiveIsAccepted() {
		assertThat(objectMapper.readValue("{\"street\":\"x\",\"isDefault\":null}", AddressRequestDTO.class).isDefault())
				.isFalse();
	}

	@Test
	@DisplayName("403 de CSRF ausente sai como ProblemDetail")
	void csrfDenialUsesProblemDetail() throws Exception {
		HttpResponse<String> response = send(to("/api/v1/addresses").header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{}")).build());

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"title\":\"Access denied\"").contains("\"status\":403")
				.contains("\"detail\"").contains("\"instance\":\"/api/v1/addresses\"");
		assertThat(response.body()).as("não pode voltar ao envelope padrão do Boot").doesNotContain("\"timestamp\"")
				.doesNotContain("\"error\"");
	}
}
