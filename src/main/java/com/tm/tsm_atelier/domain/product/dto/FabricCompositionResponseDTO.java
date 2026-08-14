package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Material;

/**
 * Os dois campos existem para separar o que é estável do que é apresentação:
 * material é a constante, que o cliente compara e envia de volta, e label é o
 * texto em português. Sem o rótulo, cada consumidor manteria a própria tabela
 * de tradução — e seria mais um lugar onde o vocabulário derrapa.
 */
public record FabricCompositionResponseDTO(Material material, String label, Integer percentage) {
}
