package com.tm.tsm_atelier.domain.order.repository;

import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecification {

	public static Specification<Order> hasStatus(OrderStatus status) {
		return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
	}

	public static Specification<Order> search(String searchTerm) {
		return (root, query, cb) -> {
			if (searchTerm == null || searchTerm.isBlank()) {
				return null;
			}

			String term = searchTerm.trim();
			String pattern = "%" + term.toLowerCase() + "%";

			List<Predicate> matches = new ArrayList<>();
			matches.add(cb.like(cb.lower(root.get("user").get("email")), pattern));
			matches.add(cb.like(cb.lower(root.get("user").get("firstName")), pattern));
			matches.add(cb.like(cb.lower(root.get("user").get("lastName")), pattern));

			parseId(term).ifPresent(id -> matches.add(cb.equal(root.get("id"), id)));

			return cb.or(matches.toArray(new Predicate[0]));
		};
	}

	public static Specification<Order> createdFrom(LocalDate from) {
		return (root, query,
				cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
	}

	public static Specification<Order> createdTo(LocalDate to) {
		return (root, query,
				cb) -> to == null ? null : cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
	}

	private static java.util.Optional<Long> parseId(String term) {
		try {
			return java.util.Optional.of(Long.parseLong(term));
		} catch (NumberFormatException notAnId) {
			return java.util.Optional.empty();
		}
	}

	private OrderSpecification() {
	}
}
