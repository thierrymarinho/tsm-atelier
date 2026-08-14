package com.tm.tsm_atelier.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DisplayName("SortWhitelist")
class SortWhitelistTest {

	private static final Set<String> ALLOWED = Set.of("id", "createdAt", "totalAmount");

	@Test
	@DisplayName("Should let an allowed field through untouched")
	void shouldAllowWhitelistedField() {
		PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

		assertThat(SortWhitelist.validate(pageable, ALLOWED)).isSameAs(pageable);
	}

	/**
	 * O caso que motivou a classe: @PageableDefault define o padrão, não um limite,
	 * e o Spring Data resolve caminhos aninhados — então a ordenação chegava até
	 * uma coluna que o endpoint nunca pretendeu expor.
	 */
	@Test
	@DisplayName("Should refuse a nested path into a related entity")
	void shouldRefuseNestedPath() {
		PageRequest pageable = PageRequest.of(0, 20, Sort.by("user.password"));

		assertThatThrownBy(() -> SortWhitelist.validate(pageable, ALLOWED)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("user.password").hasMessageContaining("createdAt");
	}

	@Test
	@DisplayName("Should refuse when only one field of a multi-field sort is disallowed")
	void shouldRefusePartiallyInvalidSort() {
		PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").and(Sort.by("user.email")));

		assertThatThrownBy(() -> SortWhitelist.validate(pageable, ALLOWED)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Should accept an unsorted pageable")
	void shouldAcceptUnsorted() {
		PageRequest pageable = PageRequest.of(0, 20);

		assertThat(SortWhitelist.validate(pageable, ALLOWED)).isSameAs(pageable);
	}
}
