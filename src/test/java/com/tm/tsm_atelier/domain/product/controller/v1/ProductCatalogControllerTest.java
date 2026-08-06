package com.tm.tsm_atelier.domain.product.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.AccessTokenDenylist;
import com.tm.tsm_atelier.security.JwtService;
import com.tm.tsm_atelier.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Os filtros deixaram de ser oito @RequestParam e passaram a ser um record
 * ligado por @ModelAttribute. E uma troca invisivel: se o binding parar de
 * funcionar, os filtros chegam nulos no service, a API responde 200 e devolve o
 * catalogo inteiro para toda busca. Nenhum teste de service pega isso, porque
 * todos chamam o metodo ja com o filtro montado. Este pega.
 */
@WebMvcTest(ProductCatalogController.class)
@Import(SecurityConfig.class)
@DisplayName("GET /api/v1/catalog/products")
class ProductCatalogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AccessTokenDenylist accessTokenDenylist;

	private static final String BASE_URL = "/api/v1/catalog/products";

	@Test
	@DisplayName("Should bind every query parameter into the search filter")
	void shouldBindEveryQueryParameterIntoTheFilter() throws Exception {
		when(productService.searchCatalog(any(), any())).thenReturn(emptyPage());

		mockMvc.perform(get(BASE_URL).param("searchTerm", "linho").param("category", "SHIRTS")
				.param("targetAudience", "MEN").param("collectionId", "7").param("minPrice", "50.00")
				.param("maxPrice", "300.00").param("isFeatured", "true").param("onSale", "true")).andExpect(status().isOk());

		ProductSearchFilter filter = captureFilter();

		assertThat(filter.searchTerm()).isEqualTo("linho");
		assertThat(filter.category()).isEqualTo(Category.SHIRTS);
		assertThat(filter.targetAudience()).isEqualTo(TargetAudience.MEN);
		assertThat(filter.collectionId()).isEqualTo(7L);
		assertThat(filter.minPrice()).isEqualByComparingTo("50.00");
		assertThat(filter.maxPrice()).isEqualByComparingTo("300.00");
		assertThat(filter.isFeatured()).isTrue();
		assertThat(filter.onSale()).isTrue();
	}

	@Test
	@DisplayName("Should leave every filter null when no query parameter is sent")
	void shouldLeaveEveryFilterNullWhenNoParameterIsSent() throws Exception {
		when(productService.searchCatalog(any(), any())).thenReturn(emptyPage());

		mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());

		ProductSearchFilter filter = captureFilter();

		// Nulo significa "nao filtra por isso". Um default acidental — string vazia
		// no lugar de nulo, por exemplo — mudaria o resultado de toda busca.
		assertThat(filter).isEqualTo(new ProductSearchFilter(null, null, null, null, null, null, null, null));
	}

	@Test
	@DisplayName("Should bind onSale alone so the storefront can list only discounted products")
	void shouldBindOnSaleAlone() throws Exception {
		when(productService.searchCatalog(any(), any())).thenReturn(emptyPage());

		mockMvc.perform(get(BASE_URL).param("onSale", "true")).andExpect(status().isOk());

		assertThat(captureFilter().onSale()).isTrue();
	}

	private ProductSearchFilter captureFilter() {
		ArgumentCaptor<ProductSearchFilter> captor = ArgumentCaptor.forClass(ProductSearchFilter.class);
		verify(productService).searchCatalog(captor.capture(), any(Pageable.class));
		return captor.getValue();
	}

	private Page<ProductSummaryDTO> emptyPage() {
		return new PageImpl<>(List.of());
	}
}
