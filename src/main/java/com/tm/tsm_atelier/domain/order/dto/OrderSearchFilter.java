package com.tm.tsm_atelier.domain.order.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Os filtros da listagem de pedidos do admin. Campo nulo significa "não filtra
 * por isso".
 *
 * <p>
 * O {@code searchTerm} é o campo único de busca do painel: quando o texto é um
 * número ele casa com o id do pedido, e no resto dos casos com e-mail e nome do
 * comprador. Duas caixas separadas — uma para id, outra para e-mail —
 * obrigariam o operador a saber de antemão o que ele tem em mãos, que é
 * justamente o que ele não sabe quando um cliente liga.
 *
 * <p>
 * As datas são {@link LocalDate} e não {@code LocalDateTime} de propósito: quem
 * filtra pedidos pensa em dias, não em instantes. A conversão para o intervalo
 * de instantes fica em OrderSpecification, com o cuidado de incluir o dia final
 * inteiro.
 */
public record OrderSearchFilter(OrderStatus status, String searchTerm,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
}
