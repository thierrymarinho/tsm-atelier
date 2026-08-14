package com.tm.tsm_atelier.domain.product.service;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.StaleResourceException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.product.dto.StockAdjustmentRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.StockResponseDTO;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estoque tem ciclo de vida próprio, e não o do cadastro do produto.
 *
 * Enquanto a única porta era o PUT de produto, ajustar um número exigia
 * carregar e reenviar a árvore inteira — todas as cores, todos os SKUs — e
 * qualquer venda ocorrida com o formulário aberto derrubava o salvamento com
 * 409, mesmo quando a edição era na descrição. A proteção estava certa; a porta
 * é que era grande demais para o que passava por ela.
 */
@Service
@RequiredArgsConstructor
public class StockService {

	private final ProductSKURepository skuRepository;
	private final AuditService auditService;

	/**
	 * O lock pessimista é o mesmo do checkout, e não é redundante com o delta: sem
	 * ele, dois +5 lidos ao mesmo tempo gravam 15 onde deveriam gravar 20 — o
	 * endpoint criado para resolver concorrência a reintroduziria.
	 *
	 * O @CacheEvict também não é cerimônia: a resposta de findBySlug é cacheada e
	 * carrega stockQuantity dentro de cada SKU, então mudar o estoque sem invalidar
	 * deixa a página do produto anunciando disponibilidade que já não existe.
	 */
	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public StockResponseDTO adjust(Long skuId, StockAdjustmentRequestDTO request) {
		ProductSKU sku = skuRepository.findByIdWithPessimisticLock(skuId)
				.orElseThrow(() -> new ResourceNotFoundException("ProductSKU", skuId));

		int current = sku.getStockQuantity();
		int target = request.delta() != null ? current + request.delta() : countedQuantity(sku, request);

		if (target < 0) {
			throw new BusinessRuleException("SKU " + sku.getSkuCode() + " has " + current
					+ " units available; this adjustment would leave it at " + target + ".");
		}

		sku.setStockQuantity(target);

		ProductSKU saved = skuRepository.saveAndFlush(sku);

		// A única alteração do painel cujo "porquê" é escolhido pelo operador, e a
		// única cuja pergunta seguinte — "por que o estoque está 7 se eu coloquei
		// 10?" — não tem resposta em nenhuma coluna de product_skus.
		auditService.recordStockChange(saved.getId(), saved.getSkuCode(), current, target, request.reason());

		return new StockResponseDTO(saved.getId(), saved.getSkuCode(), saved.getStockQuantity(), saved.getVersion());
	}

	/**
	 * A versão é lida depois do lock, então ela é necessariamente a atual — o
	 * conflito detectado aqui é real, e não uma janela de leitura.
	 */
	private int countedQuantity(ProductSKU sku, StockAdjustmentRequestDTO request) {
		if (!request.version().equals(sku.getVersion())) {
			throw new StaleResourceException("SKU " + sku.getSkuCode() + " is now at " + sku.getStockQuantity()
					+ " units (version " + sku.getVersion() + "), but the count was made against version "
					+ request.version() + ". Reload and confirm before saving.");
		}

		return request.absolute();
	}
}
