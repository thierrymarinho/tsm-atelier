package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class ProductCatalogController {

	private final ProductService productService;

	@GetMapping
	public ResponseEntity<Page<ProductSummaryDTO>> search(@RequestParam(required = false) String searchTerm,
			@RequestParam(required = false) Category category,
			@RequestParam(required = false) TargetAudience targetAudience,
			@RequestParam(required = false) Long collectionId, @RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) Boolean isFeatured,
			@PageableDefault(size = 12) Pageable pageable) {

		return ResponseEntity.ok(productService.searchCatalog(searchTerm, category, targetAudience, collectionId,
				minPrice, maxPrice, isFeatured, pageable));
	}

	@GetMapping("/slug/{slug}")
	public ResponseEntity<ProductResponseDTO> findBySlug(@PathVariable String slug) {
		Long id = extractIdFromSlug(slug);
		ProductResponseDTO product = productService.findById(id);

		if (!product.slug().equals(slug)) {
			return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
					.header("Location", "/api/v1/catalog/products/slug/" + product.slug()).build();
		}

		return ResponseEntity.ok(product);
	}

	private Long extractIdFromSlug(String slug) {
		try {
			String[] parts = slug.split("-");
			return Long.parseLong(parts[parts.length - 1]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			throw new ResourceNotFoundException("Product", slug);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findById(id));
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getCategories(@RequestParam(required = false) TargetAudience targetAudience) {
		return ResponseEntity.ok(Category.getByTargetAudience(targetAudience));
	}
}
