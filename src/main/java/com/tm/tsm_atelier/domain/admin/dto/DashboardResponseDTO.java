package com.tm.tsm_atelier.domain.admin.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import java.util.List;
import java.util.Map;

/**
 * Tudo o que a home do painel precisa, numa requisição. Sem isto a tela teria
 * que baixar todos os pedidos e somar no navegador — o que funciona com
 * cinquenta pedidos e para de funcionar exatamente quando a loja começa a dar
 * certo.
 *
 * @param ordersByStatus
 *            contagem por status, com todos os status presentes. Um status
 *            ausente do mapa obrigaria a interface a tratar "sem pedidos" e
 *            "chave que não veio" como o mesmo caso.
 * @param lowStockCount
 *            total de SKUs abaixo do limiar; lowStock traz só uma página, então
 *            a tela consegue dizer "20 de 47" em vez de insinuar que são 20.
 * @param lowStockPageSize
 *            quantas linhas cabem numa página de lowStock. Vai na resposta para
 *            o cliente calcular o número de páginas sem repetir uma constante
 *            do servidor — repetida, ela viraria uma paginação silenciosamente
 *            errada no dia em que o tamanho mudasse aqui.
 * @param lowStockPage
 *            índice da página devolvida, base 0. Ecoado para a tela conseguir
 *            se reencontrar depois de um recarregamento.
 */
public record DashboardResponseDTO(Map<OrderStatus, Long> ordersByStatus, RevenueDTO revenue,
		List<LowStockSkuDTO> lowStock, long lowStockCount, int lowStockPageSize, int lowStockPage) {
}
