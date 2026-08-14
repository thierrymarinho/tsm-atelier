package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "UPDATE product_colors SET deleted_at = NULL WHERE product_id = :productId AND deleted_at = :deletedAt", nativeQuery = true)
	int restoreColorsOfProduct(@Param("productId") Long productId, @Param("deletedAt") LocalDateTime deletedAt);
}
