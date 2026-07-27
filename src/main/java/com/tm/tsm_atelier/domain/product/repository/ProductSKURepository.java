package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSKURepository extends JpaRepository<ProductSKU, Long> {
	Optional<ProductSKU> findBySkuCode(String skuCode);

	@org.springframework.data.jpa.repository.Query("SELECT s.skuCode FROM ProductSKU s WHERE s.skuCode IN :codes")
	java.util.List<String> findExistingSkuCodes(
			@org.springframework.data.repository.query.Param("codes") java.util.List<String> codes);

	boolean existsBySkuCodeAndIdNot(String skuCode, Long id);
}
