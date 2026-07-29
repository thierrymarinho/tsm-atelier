package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductSKURepository extends JpaRepository<ProductSKU, Long> {
	Optional<ProductSKU> findBySkuCode(String skuCode);

	@Query("SELECT s.skuCode FROM ProductSKU s WHERE s.skuCode IN :codes")
	List<String> findExistingSkuCodes(@Param("codes") List<String> codes);

	boolean existsBySkuCodeAndIdNot(String skuCode, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ProductSKU s WHERE s.id = :id")
	Optional<ProductSKU> findByIdWithPessimisticLock(@Param("id") Long id);
}
