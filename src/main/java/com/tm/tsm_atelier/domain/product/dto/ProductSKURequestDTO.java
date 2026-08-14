package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Não há {@code skuCode} aqui: o código é interno e o backend o gera. Enquanto
 * era digitado, o admin preenchia produto, cores, imagens e tamanhos inteiros
 * para ser recusado com 409 por um código ocupado que ele não tinha como
 * enxergar — e um par duplicado dentro do próprio payload passava pela
 * validação e morria no índice. Ver {@code ProductService.generateSkuCode}.
 *
 * <p>
 * O {@code stockQuantity} vale apenas para SKU novo, como estoque inicial.
 * Enviá-lo com {@code id} preenchido é recusado — não ignorado — apontando
 * {@code PATCH /api/v1/admin/skus/{id}/stock}.
 *
 * <p>
 * Regra condicional, por isso validada em ProductService e não com @NotNull
 * aqui.
 *
 * <p>
 * A separação é o que dispensa este payload de carregar versão: enquanto o
 * formulário escrevia estoque, salvar uma edição de descrição podia ressuscitar
 * unidades vendidas no meio tempo, e a única defesa era exigir a versão de cada
 * SKU e recusar o salvamento inteiro com 409. Sem estoque no caminho, a corrida
 * deixa de existir — o cadastro e o número que muda a cada venda param de
 * compartilhar a mesma gravação.
 */
public record ProductSKURequestDTO(Long id,

		@NotNull(message = "Size is required") ProductSize size,

		@Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity) {
}
