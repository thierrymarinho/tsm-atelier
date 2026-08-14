package com.tm.tsm_atelier.domain.admin.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;

/**
 * Um SKU perto de acabar. Carrega produto, cor e tamanho porque um alerta que
 * diz só o código do SKU obriga o operador a ir procurar de que peça se trata —
 * e o skuId porque a ação seguinte é o PATCH /api/v1/admin/skus/{id}/stock, que
 * a tela deve poder oferecer direto da linha.
 *
 *
 * A version vai junto pelo mesmo motivo, levado até o fim: sem ela a linha só
 * consegue oferecer delta — "entrou 3", "saiu 2" — e não a contagem física, que
 * é o gesto mais comum de quem confere prateleira e o único que
 * StockAdjustmentRequestDTO condiciona a uma versão. Deixá-la de fora empurrava
 * o operador para o cálculo proibido contado − exibido, que é o lost update
 * disfarçado que o campo existe para pegar.
 */
public record LowStockSkuDTO(Long skuId, String skuCode, Long productId, String productName, String colorName,
		ProductSize size, Integer stockQuantity, Long version) {
}
