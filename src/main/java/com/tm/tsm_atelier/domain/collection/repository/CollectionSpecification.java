package com.tm.tsm_atelier.domain.collection.repository;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import org.springframework.data.jpa.domain.Specification;

public class CollectionSpecification {

	public static Specification<Collection> isActive() {
		return (root, query, cb) -> cb.isTrue(root.get("active"));
	}

	public static Specification<Collection> hasPosition(DisplayPosition position) {
		return (root, query, cb) -> position == null ? null : cb.equal(root.get("displayPosition"), position);
	}

	public static Specification<Collection> hasTargetAudience(TargetAudience audience) {
		return (root, query, cb) -> audience == null ? null : cb.equal(root.get("targetAudience"), audience);
	}
}
