package com.tm.tsm_atelier.domain.product.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Retirar um produto da promoção é mandar o update sem o preço promocional — o
 * PUT é de substituição total e trata campo ausente como remoção, mesma
 * semântica já usada por collectionId.
 *
 * Isso depende inteiramente de o MapStruct estar em SET_TO_NULL. Trocar para
 * IGNORE é uma mudança de uma palavra, parece inofensiva e costuma ser feita
 * para "não apagar campos sem querer" — e tornaria a promoção impossível de
 * remover: o admin salvaria, receberia 200, e o preço promocional continuaria
 * lá. Sem erro nenhum. Este teste é o que quebra nesse dia.
 */
@DisplayName("Promotion removal through the product update")
class ProductPromotionRemovalTest {

	private final ProductMapper mapper = new ProductMapperImpl();

	@Test
	@DisplayName("Should clear the promotional price when the update omits it")
	void shouldClearThePromotionalPriceWhenTheUpdateOmitsIt() {
		Product product = Product.builder().id(1L).name("Camisa").price(new BigDecimal("200.00"))
				.promotionalPrice(new BigDecimal("150.00")).build();

		mapper.updateEntityFromRequest(requestWithPromotionalPrice(null), product);

		assertThat(product.getPromotionalPrice()).isNull();
		assertThat(product.getPrice()).isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("Should apply the promotional price when the update carries it")
	void shouldApplyThePromotionalPriceWhenTheUpdateCarriesIt() {
		Product product = Product.builder().id(1L).name("Camisa").price(new BigDecimal("200.00")).build();

		mapper.updateEntityFromRequest(requestWithPromotionalPrice(new BigDecimal("120.00")), product);

		assertThat(product.getPromotionalPrice()).isEqualByComparingTo("120.00");
		assertThat(product.getEffectivePrice()).isEqualByComparingTo("120.00");
	}

	private ProductRequestDTO requestWithPromotionalPrice(BigDecimal promotionalPrice) {
		return new ProductRequestDTO("Camisa", "Descrição", List.of(), List.of(), new BigDecimal("200.00"),
				promotionalPrice, null, Category.T_SHIRTS, TargetAudience.MEN, true, false, List.of());
	}
}
