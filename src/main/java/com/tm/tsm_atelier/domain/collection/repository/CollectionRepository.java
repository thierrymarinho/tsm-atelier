package com.tm.tsm_atelier.domain.collection.repository;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {

	boolean existsByNameAndTargetAudience(String name, TargetAudience targetAudience);

	java.util.Optional<Collection> findByNameAndTargetAudience(String name, TargetAudience targetAudience);

	java.util.Optional<Collection> findByDisplayPositionAndTargetAudience(DisplayPosition displayPosition,
			TargetAudience targetAudience);

	java.util.Optional<Collection> findByDisplayPosition(DisplayPosition displayPosition);
}
