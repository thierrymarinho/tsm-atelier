package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.Product;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@EntityGraph(attributePaths = {"collection", "colors", "colors.skus"})
	Optional<Product> findByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"collection", "colors", "colors.skus"})
	Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

	@EntityGraph(attributePaths = {"collection"})
	Page<Product> findAll(@NonNull Specification<Product> spec, @NonNull Pageable pageable);

	Boolean existsByNameAndDeletedAtIsNull(String name);

	@Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.colors WHERE p IN :products")
	List<Product> fetchColorsForProducts(@Param("products") List<Product> products);
}
