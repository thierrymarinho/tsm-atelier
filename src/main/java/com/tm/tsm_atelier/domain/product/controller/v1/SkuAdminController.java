package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.domain.product.dto.StockAdjustmentRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.StockResponseDTO;
import com.tm.tsm_atelier.domain.product.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O SKU aparece aqui como recurso próprio, e não aninhado sob o produto: o id é
 * único globalmente, então a rota aninhada só acrescentaria uma validação de
 * pertencimento sem nenhuma garantia nova — o acesso já é decidido por papel no
 * SecurityConfig, que cobre {@code /api/v1/admin/**}.
 */
@RestController
@RequestMapping("/api/v1/admin/skus")
@RequiredArgsConstructor
public class SkuAdminController {

	private final StockService stockService;

	@PatchMapping("/{id}/stock")
	public ResponseEntity<StockResponseDTO> adjustStock(@PathVariable Long id,
			@RequestBody @Valid StockAdjustmentRequestDTO request) {
		return ResponseEntity.ok(stockService.adjust(id, request));
	}
}
