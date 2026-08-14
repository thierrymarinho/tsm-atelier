package com.tm.tsm_atelier.domain.collection.repository;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {

	Optional<Collection> findByNameAndTargetAudience(String name, TargetAudience targetAudience);

	Optional<Collection> findBySlug(String slug);

	Optional<Collection> findByDisplayPositionAndTargetAudience(DisplayPosition displayPosition,
			TargetAudience targetAudience);

	Optional<Collection> findByDisplayPosition(DisplayPosition displayPosition);

	@Query(value = "SELECT * FROM collections WHERE id = :id", nativeQuery = true)
	Optional<Collection> findByIdIncludingDeleted(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "UPDATE collections SET deleted_at = NULL, display_position = 'NONE' WHERE id = :id", nativeQuery = true)
	int restoreCollection(@Param("id") Long id);

	@Query(value = """
			SELECT id FROM collections
			WHERE name = :name AND target_audience = :targetAudience AND deleted_at IS NOT NULL
			""", nativeQuery = true)
	Optional<Long> findDeletedIdByNameAndTargetAudience(@Param("name") String name,
			@Param("targetAudience") String targetAudience);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM collections WHERE slug = :slug)", nativeQuery = true)
	boolean existsBySlugIncludingDeleted(@Param("slug") String slug);
}
