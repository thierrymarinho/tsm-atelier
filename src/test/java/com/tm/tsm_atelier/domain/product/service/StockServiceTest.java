package com.tm.tsm_atelier.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.StaleResourceException;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.product.dto.StockAdjustmentRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.StockResponseDTO;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.enums.StockChangeReason;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService.adjust()")
class StockServiceTest {

	@Mock
	private ProductSKURepository skuRepository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private StockService stockService;

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("Should add a delta to the current quantity")
	void shouldApplyDelta() {
		ProductSKU sku = aSku(10, 4L);
		givenSku(sku);

		StockResponseDTO response = stockService.adjust(1L,
				new StockAdjustmentRequestDTO(20, null, null, StockChangeReason.RESTOCK));

		assertThat(response.stockQuantity()).isEqualTo(30);
		assertThat(sku.getStockQuantity()).isEqualTo(30);
	}

	/**
	 * O ponto do delta: ele não precisa saber o total. Se o estoque tiver caído de
	 * 10 para 7 entre a leitura da tela e o envio, o "+20" ainda significa vinte
	 * unidades a mais — nada a recarregar, nada a conciliar.
	 */
	@Test
	@DisplayName("Should not require a version for a delta")
	void deltaNeedsNoVersion() {
		givenSku(aSku(7, 9L));

		StockResponseDTO response = stockService.adjust(1L,
				new StockAdjustmentRequestDTO(20, null, null, StockChangeReason.RESTOCK));

		assertThat(response.stockQuantity()).isEqualTo(27);
	}

	@Test
	@DisplayName("Should take the counted quantity when the version still matches")
	void shouldApplyAbsoluteWithCurrentVersion() {
		ProductSKU sku = aSku(10, 4L);
		givenSku(sku);

		StockResponseDTO response = stockService.adjust(1L,
				new StockAdjustmentRequestDTO(null, 7, 4L, StockChangeReason.INVENTORY_COUNT));

		assertThat(response.stockQuantity()).isEqualTo(7);
	}

	/**
	 * Contagem física feita sobre uma leitura vencida é o lost update de sempre: o
	 * admin conta 7 na prateleira, três unidades são vendidas antes de ele salvar,
	 * e gravar 7 devolveria ao estoque o que já saiu.
	 */
	@Test
	@DisplayName("Should refuse a count made against an older version")
	void shouldRefuseStaleCount() {
		ProductSKU sku = aSku(7, 5L);
		when(skuRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(sku));

		assertThatThrownBy(() -> stockService.adjust(1L,
				new StockAdjustmentRequestDTO(null, 10, 4L, StockChangeReason.INVENTORY_COUNT)))
				.isInstanceOf(StaleResourceException.class).hasMessageContaining("version 5");

		verify(skuRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Should refuse an adjustment that would push the stock below zero")
	void shouldRefuseNegativeResult() {
		ProductSKU sku = aSku(2, 1L);
		when(skuRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(sku));

		assertThatThrownBy(
				() -> stockService.adjust(1L, new StockAdjustmentRequestDTO(-5, null, null, StockChangeReason.DAMAGE)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("2 units available");

		assertThat(sku.getStockQuantity()).isEqualTo(2);
	}

	/**
	 * O SKU tem @SQLRestriction, então um removido do catálogo simplesmente não é
	 * encontrado — ajustar estoque de item fora de venda é 404, e não uma gravação
	 * silenciosa numa linha invisível.
	 */
	@Test
	@DisplayName("Should not find a sku that was removed from the catalog")
	void shouldNotAdjustARemovedSku() {
		when(skuRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> stockService.adjust(1L, new StockAdjustmentRequestDTO(1, null, null, StockChangeReason.RESTOCK)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// -------------------------------------------------- contrato do payload

	@Test
	@DisplayName("Should reject a payload carrying both delta and absolute")
	void shouldRejectBothOperations() {
		assertThat(VALIDATOR.validate(new StockAdjustmentRequestDTO(3, 7, 4L, StockChangeReason.CORRECTION)))
				.extracting(violation -> violation.getMessage())
				.contains("Send exactly one of 'delta' (non-zero) or 'absolute'.");
	}

	@Test
	@DisplayName("Should reject a payload carrying neither")
	void shouldRejectEmptyOperation() {
		assertThat(VALIDATOR.validate(new StockAdjustmentRequestDTO(null, null, null, StockChangeReason.CORRECTION)))
				.isNotEmpty();
	}

	/**
	 * Sem esta regra o cliente poderia mandar um valor absoluto sem versão e a
	 * proteção contra contagem vencida viraria algo que se desliga por omissão —
	 * exatamente o que se quis evitar quando ela vivia no PUT de produto.
	 */
	@Test
	@DisplayName("Should reject an absolute quantity sent without a version")
	void shouldRejectAbsoluteWithoutVersion() {
		assertThat(VALIDATOR.validate(new StockAdjustmentRequestDTO(null, 7, null, StockChangeReason.INVENTORY_COUNT)))
				.extracting(violation -> violation.getMessage())
				.contains("'absolute' requires the 'version' returned by the API for this SKU.");
	}

	@Test
	@DisplayName("Should reject a zero delta")
	void shouldRejectZeroDelta() {
		assertThat(VALIDATOR.validate(new StockAdjustmentRequestDTO(0, null, null, StockChangeReason.CORRECTION)))
				.isNotEmpty();
	}

	@Test
	@DisplayName("Should require a reason")
	void shouldRequireReason() {
		assertThat(VALIDATOR.validate(new StockAdjustmentRequestDTO(3, null, null, null)))
				.extracting(violation -> violation.getPropertyPath().toString()).contains("reason");
	}

	private ProductSKU aSku(int stock, long version) {
		return ProductSKU.builder().id(1L).skuCode("SKU-1").size(ProductSize.M).stockQuantity(stock).version(version)
				.build();
	}

	private void givenSku(ProductSKU sku) {
		when(skuRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(sku));
		when(skuRepository.saveAndFlush(sku)).thenReturn(sku);
	}
}
