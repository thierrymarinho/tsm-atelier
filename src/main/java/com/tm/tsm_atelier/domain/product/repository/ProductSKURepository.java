package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSKURepository extends JpaRepository<ProductSKU, Long> {
	Optional<ProductSKU> findBySkuCode(String skuCode);

	@Query(value = "SELECT nextval('sku_code_seq')", nativeQuery = true)
	Long nextSkuCodeNumber();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ProductSKU s WHERE s.id = :id")
	Optional<ProductSKU> findByIdWithPessimisticLock(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE product_skus SET deleted_at = NULL
			WHERE deleted_at = :deletedAt
			  AND product_color_id IN (SELECT id FROM product_colors WHERE product_id = :productId)
			""", nativeQuery = true)
	int restoreSkusOfProduct(@Param("productId") Long productId, @Param("deletedAt") java.time.LocalDateTime deletedAt);

	@Query(value = """
			SELECT s.sku_code FROM product_skus s
			JOIN product_colors c ON c.id = s.product_color_id
			WHERE c.product_id = :productId
			  AND s.deleted_at = :deletedAt
			  AND EXISTS (SELECT 1 FROM product_skus live
			              WHERE live.sku_code = s.sku_code AND live.deleted_at IS NULL)
			""", nativeQuery = true)
	List<String> findSkuCodesBlockingRestore(@Param("productId") Long productId,
			@Param("deletedAt") java.time.LocalDateTime deletedAt);

	@Query("""
			SELECT s FROM ProductSKU s
			JOIN s.productColor c JOIN c.product p
			WHERE p.deletedAt IS NULL AND p.active = true AND s.stockQuantity <= :threshold
			ORDER BY s.stockQuantity ASC, s.id ASC
			""")
	List<ProductSKU> findLowStock(@Param("threshold") int threshold, org.springframework.data.domain.Pageable pageable);

	@Query("""
			SELECT COUNT(s) FROM ProductSKU s
			JOIN s.productColor c JOIN c.product p
			WHERE p.deletedAt IS NULL AND p.active = true AND s.stockQuantity <= :threshold
			""")
	long countLowStock(@Param("threshold") int threshold);
}
