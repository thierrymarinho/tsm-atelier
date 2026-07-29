package com.tm.tsm_atelier.domain.product.controller.v1;

import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static com.tm.tsm_atelier.common.builders.ProductResponseDTOBuilder.aProductResponse;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
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

	private static final String BASE_URL = "/api/v1/admin/products";

	@Nested
	@DisplayName("POST " + BASE_URL)
	class CreateProduct {

		@Test
		@DisplayName("Deve retornar 201 quando o produto é criado com sucesso")
		void shouldReturn201WhenProductIsCreatedSuccessfully() throws Exception {
			// Arrange
			ProductRequestDTO requestDTO = aProductRequest().build();
			ProductResponseDTO responseDTO = aProductResponse().withId(1L).withName(requestDTO.name()).build();

			when(productService.create(any(ProductRequestDTO.class))).thenReturn(responseDTO);

			// Act & Assert
			mockMvc.perform(post(BASE_URL).with(user("admin").roles("ADMIN")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDTO)))
					.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
					.andExpect(jsonPath("$.name").value(requestDTO.name()));

			verify(productService, times(1)).create(any(ProductRequestDTO.class));
		}

		@Test
		@DisplayName("Deve retornar 422 (Unprocessable Entity) quando os dados do produto são inválidos")
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

		@Test
		@DisplayName("Deve retornar 403 quando o usuário CUSTOMER tenta criar produto")
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
		@DisplayName("Deve retornar 404 quando a coleção vinculada não for encontrada")
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
		@DisplayName("Deve retornar 200 e a lista de produtos")
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
		@DisplayName("Deve retornar 200 e o produto quando encontrado")
		void shouldReturn200AndProductWhenFound() {
			// Cenário a ser implementado
		}

		@Test
		@DisplayName("Deve retornar 404 quando o produto não for encontrado")
		void shouldReturn404WhenProductNotFound() {
			// Cenário a ser implementado
		}
	}
}
