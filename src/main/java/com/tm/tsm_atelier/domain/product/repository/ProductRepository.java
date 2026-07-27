package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@EntityGraph(attributePaths = {"collection", "colors", "colors.skus"})
	Optional<Product> findByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"collection", "colors", "colors.skus"})
	Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

	// Usamos EntityGraph apenas para collection no paginado para evitar in-memory
	// pagination
	@EntityGraph(attributePaths = {"collection"})
	Page<Product> findAll(@Nullable Specification<Product> spec, Pageable pageable);

	Boolean existsByNameAndDeletedAtIsNull(String name);

	@org.springframework.data.jpa.repository.Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.colors WHERE p IN :products")
	java.util.List<Product> fetchColorsForProducts(
			@org.springframework.data.repository.query.Param("products") java.util.List<Product> products);
}
