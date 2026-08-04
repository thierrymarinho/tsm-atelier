package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSKURepository extends JpaRepository<ProductSKU, Long> {
	Optional<ProductSKU> findBySkuCode(String skuCode);

	/**
	 * Consultas nativas de propósito: o índice único de sku_code no banco também
	 * cobre as linhas soft-deleted, então a validação de duplicidade precisa
	 * enxergar registros que o @SQLRestriction da entidade esconde. Sem isso o
	 * cadastro passaria na validação e só quebraria no INSERT.
	 */
	@Query(value = "SELECT sku_code FROM product_skus WHERE sku_code IN (:codes)", nativeQuery = true)
	List<String> findExistingSkuCodes(@Param("codes") List<String> codes);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM product_skus WHERE sku_code = :skuCode)", nativeQuery = true)
	boolean existsBySkuCodeIncludingDeleted(@Param("skuCode") String skuCode);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM product_skus WHERE sku_code = :skuCode AND id <> :id)", nativeQuery = true)
	boolean existsBySkuCodeAndIdNot(@Param("skuCode") String skuCode, @Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ProductSKU s WHERE s.id = :id")
	Optional<ProductSKU> findByIdWithPessimisticLock(@Param("id") Long id);
}
