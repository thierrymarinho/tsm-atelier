package com.tm.tsm_atelier.domain.product.controller.v1;

import static com.tm.tsm_atelier.common.builders.AdminProductResponseDTOBuilder.anAdminProductResponse;
import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.JwtService;
import com.tm.tsm_atelier.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductAdminController.class)
@Import(SecurityConfig.class)
class ProductAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private com.tm.tsm_atelier.security.AccessTokenDenylist accessTokenDenylist;

	private static final String BASE_URL = "/api/v1/admin/products";

	@Nested
	@DisplayName("POST " + BASE_URL)
	class CreateProduct {

		@Test
		@DisplayName("Should return 201 when the product is created successfully")
		void shouldReturn201WhenProductIsCreatedSuccessfully() throws Exception {
			// Arrange
			ProductRequestDTO requestDTO = aProductRequest().build();
			AdminProductResponseDTO responseDTO = anAdminProductResponse().withId(1L).withName(requestDTO.name())
					.build();

			when(productService.create(any(ProductRequestDTO.class))).thenReturn(responseDTO);

			// Act & Assert
			mockMvc.perform(post(BASE_URL).with(user("admin").roles("ADMIN")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDTO)))
					.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
					.andExpect(jsonPath("$.name").value(requestDTO.name()));

			verify(productService, times(1)).create(any(ProductRequestDTO.class));
		}

		@Test
		@DisplayName("Should return 422 (Unprocessable Entity) when the product data is invalid")
		void shouldReturn422WhenProductDataIsInvalid() throws Exception {
			// Arrange
			ProductRequestDTO requestDTO = aProductRequest().withName("").withPrice(new java.math.BigDecimal("-10.00"))
					.withColors(java.util.Collections.emptyList()).build();

			// Act
			mockMvc.perform(post(BASE_URL).with(user("admin").roles("ADMIN")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDTO)))

					// Assert
					.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.status").value(422))
					.andExpect(jsonPath("$.fields.name").value("Product name is required"))
					.andExpect(jsonPath("$.fields.price").value("Price must be greater than zero"))
					.andExpect(jsonPath("$.fields.colors").value("Product must have at least one color"));

			verify(productService, never()).create(any(ProductRequestDTO.class));
		}

		/**
		 * Um material fora do enum falha na desserializacao, antes de qualquer
		 * validacao — e a resposta herdada do Spring diria apenas que o corpo nao pode
		 * ser lido. Num formulario de cadastro isso e um beco sem saida: o admin nao
		 * descobre nem que campo errou, nem que existe uma lista fechada.
		 *
		 * <p>
		 * O indice no nome do campo importa: numa composicao de tres materiais, saber
		 * que "algum material e invalido" nao diz qual linha corrigir.
		 */
		@Test
		@DisplayName("Should name the field and list the options when the material is not in the enum")
		void shouldExplainAnUnknownMaterial() throws Exception {
			String body = objectMapper.writeValueAsString(aProductRequest().build()).replace("\"COTTON\"",
					"\"Algodao\"");

			mockMvc.perform(post(BASE_URL).with(user("admin").roles("ADMIN")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.field").value("fabricCompositions[0].material"))
					.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Algodao")))
					.andExpect(jsonPath("$.allowedValues").value(org.hamcrest.Matchers.hasItem("COTTON")));

			verify(productService, never()).create(any());
		}

		@Test
		@DisplayName("Should return 403 when a CUSTOMER user tries to create a product")
		void shouldReturn403WhenCustomerTriesToCreate() throws Exception {
			// Arrange
			ProductRequestDTO requestDTO = aProductRequest().build();

			// Act & Assert
			mockMvc.perform(post(BASE_URL).with(user("customer").roles("CUSTOMER")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDTO)))
					.andExpect(status().isForbidden());

			verify(productService, never()).create(any());
		}

		@Test
		@DisplayName("Should return 404 when the linked collection is not found")
		void shouldReturn404WhenCollectionIsNotFound() throws Exception {
			// Arrange
			ProductRequestDTO requestDTO = aProductRequest().withCollectionId(1L).build();

			when(productService.create(any(ProductRequestDTO.class))).thenThrow(
					new com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException("Collection", 1L));

			// Act
			mockMvc.perform(post(BASE_URL).with(user("admin").roles("ADMIN")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDTO)))

					// Assert
					.andExpect(status().isNotFound());

			verify(productService, times(1)).create(any(ProductRequestDTO.class));
		}
	}

	@Nested
	@DisplayName("GET " + BASE_URL)
	class FindAllProducts {

		@Test
		@DisplayName("Should return 200 and the product list")
		void shouldReturn200AndProductList() {
			// Arrange

			// Act

			// Assert
		}
	}

	@Nested
	@DisplayName("GET " + BASE_URL + "/{id}")
	class FindProductById {

		@Test
		@DisplayName("Should return 200 and the product when found")
		void shouldReturn200AndProductWhenFound() {
			// Cenário a ser implementado
		}

		@Test
		@DisplayName("Should return 404 when the product is not found")
		void shouldReturn404WhenProductNotFound() {
			// Cenário a ser implementado
		}
	}
}
