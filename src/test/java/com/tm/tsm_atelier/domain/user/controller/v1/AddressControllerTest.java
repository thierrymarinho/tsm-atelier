package com.tm.tsm_atelier.domain.user.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.common.exception.GlobalExceptionHandler;
import com.tm.tsm_atelier.common.exception.custom.AddressLimitExceededException;
import com.tm.tsm_atelier.domain.user.dto.AddressRequestDTO;
import com.tm.tsm_atelier.domain.user.dto.AddressResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.enums.State;
import com.tm.tsm_atelier.domain.user.service.AddressService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AddressService addressService;

	@InjectMocks
	private AddressController addressController;

	private ObjectMapper objectMapper = new ObjectMapper();

	private User mockUser;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setId(UUID.randomUUID());

		// Custom argument resolver to inject @AuthenticationPrincipal User
		HandlerMethodArgumentResolver putAuthenticationPrincipal = new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.getParameterType().isAssignableFrom(User.class);
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
				return mockUser;
			}
		};

		mockMvc = MockMvcBuilders.standaloneSetup(addressController).setControllerAdvice(new GlobalExceptionHandler())
				.setCustomArgumentResolvers(putAuthenticationPrincipal).build();
	}

	@Test
	@DisplayName("POST /api/v1/addresses - should return 201 when created successfully")
	void shouldCreateAddress() throws Exception {
		AddressRequestDTO req = new AddressRequestDTO("Street", "1", "C", "N", "City", State.SP, "12345678", true);
		AddressResponseDTO res = new AddressResponseDTO(1L, "Street", "1", "C", "N", "City", State.SP, "12345678",
				true);

		when(addressService.create(any(), any())).thenReturn(res);

		mockMvc.perform(post("/api/v1/addresses").header("Authorization", "Bearer token")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.postalCode").value("12345678"));
	}

	@Test
	@DisplayName("POST /api/v1/addresses - should return 422 when the limit is exceeded")
	void shouldReturn422WhenLimitExceeded() throws Exception {
		AddressRequestDTO req = new AddressRequestDTO("Street", "1", "C", "N", "City", State.SP, "12345678", true);

		when(addressService.create(any(), any())).thenThrow(new AddressLimitExceededException("Limit exceeded"));

		mockMvc.perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Limit exceeded"));
	}

	@Test
	@DisplayName("GET /api/v1/addresses - should return 200 with the address list")
	void shouldReturnAddressList() throws Exception {
		when(addressService.findAllByUser(any())).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/addresses"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("PATCH /api/v1/addresses/{id}/default - should return 200")
	void shouldSetDefault() throws Exception {
		Long id = 1L;
		AddressResponseDTO res = new AddressResponseDTO(id, "S", "1", null, "N", "C", State.SP, "1", true);
		when(addressService.setDefault(any(), eq(id))).thenReturn(res);

		mockMvc.perform(patch("/api/v1/addresses/" + id + "/default")).andExpect(status().isOk())
				.andExpect(jsonPath("$.isDefault").value(true));
	}

	@Test
	@DisplayName("DELETE /api/v1/addresses/{id} - should return 204")
	void shouldDeleteAddress() throws Exception {
		Long id = 1L;
		doNothing().when(addressService).delete(any(), eq(id));

		mockMvc.perform(delete("/api/v1/addresses/" + id)).andExpect(status().isNoContent());

		verify(addressService).delete(any(), eq(id));
	}
}
