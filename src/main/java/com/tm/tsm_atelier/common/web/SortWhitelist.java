package com.tm.tsm_atelier.common.web;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Restringe a ordenação de um {@link Pageable} aos campos que o endpoint
 * realmente oferece.
 *
 * <p>
 * {@code @PageableDefault} define o padrão, não um limite: o cliente
 * sobrescreve pela query string, e o Spring Data resolve caminhos aninhados.
 * Sem esta validação, {@code ?sort=user.password,asc} era aceito — ordenar por
 * um campo não revela o valor diretamente, mas é um vetor conhecido de
 * inferência sobre dado sensível, e uma propriedade inexistente virava
 * {@code PropertyReferenceException} e saía como 500.
 */
public final class SortWhitelist {

	private SortWhitelist() {
	}

	/**
	 * Devolve o próprio pageable quando toda a ordenação é permitida, e recusa a
	 * requisição inteira quando não é. Recusar é melhor do que ignorar em silêncio:
	 * uma ordenação descartada sem aviso faz o cliente exibir a lista na ordem
	 * errada achando que pediu certo.
	 */
	public static Pageable validate(Pageable pageable, Set<String> allowedProperties) {
		for (Sort.Order order : pageable.getSort()) {
			if (!allowedProperties.contains(order.getProperty())) {
				throw new BusinessRuleException("Cannot sort by '" + order.getProperty() + "'. Allowed fields: "
						+ String.join(", ", new TreeSet<>(allowedProperties)) + ".");
			}
		}
		return pageable;
	}
}
