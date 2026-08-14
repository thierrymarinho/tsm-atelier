package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

	public static Specification<Product> isNotDeleted() {
		return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
	}

	public static Specification<Product> isActive() {
		return (root, query, cb) -> cb.isTrue(root.get("active"));
	}

	public static Specification<Product> isFeatured(Boolean isFeatured) {
		return (root, query, cb) -> isFeatured == null ? null : cb.equal(root.get("featured"), isFeatured);
	}

	public static Specification<Product> isOnSale(Boolean onSale) {
		return (root, query, cb) -> {
			if (onSale == null) {
				return null;
			}
			return onSale ? cb.isNotNull(root.get("promotionalPrice")) : cb.isNull(root.get("promotionalPrice"));
		};
	}

	public static Specification<Product> hasCategory(Category category) {
		return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
	}

	public static Specification<Product> hasTargetAudience(TargetAudience targetAudience) {
		return (root, query,
				cb) -> targetAudience == null ? null : cb.equal(root.get("targetAudience"), targetAudience);
	}

	public static Specification<Product> hasCollectionId(Long collectionId) {
		return (root, query,
				cb) -> collectionId == null ? null : cb.equal(root.get("collection").get("id"), collectionId);
	}

	public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			Expression<BigDecimal> effectivePrice = cb.coalesce(root.get("promotionalPrice"), root.get("price"));

			if (minPrice != null) {
				predicates.add(cb.greaterThanOrEqualTo(effectivePrice, minPrice));
			}
			if (maxPrice != null) {
				predicates.add(cb.lessThanOrEqualTo(effectivePrice, maxPrice));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	public static Specification<Product> search(String searchTerm) {
		return (root, query, cb) -> {
			if (searchTerm == null || searchTerm.isBlank()) {
				return null;
			}
			String pattern = "%" + searchTerm.toLowerCase() + "%";
			return cb.or(cb.like(cb.lower(root.get("name")), pattern),
					cb.like(cb.lower(root.get("description")), pattern));
		};
	}
}
