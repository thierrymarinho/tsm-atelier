package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

	private final ProductService productService;

	@PostMapping
	public ResponseEntity<ProductResponseDTO> create(@RequestBody @Valid ProductRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
	}

	@GetMapping
	public ResponseEntity<org.springframework.data.domain.Page<com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO>> search(
			@RequestParam(required = false) String searchTerm,
			@RequestParam(required = false) com.tm.tsm_atelier.domain.product.enums.Category category,
			@RequestParam(required = false) com.tm.tsm_atelier.domain.product.enums.TargetAudience targetAudience,
			@RequestParam(required = false) Long collectionId,
			@RequestParam(required = false) java.math.BigDecimal minPrice,
			@RequestParam(required = false) java.math.BigDecimal maxPrice,
			@RequestParam(required = false) Boolean isFeatured,
			@org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
		return ResponseEntity.ok(productService.searchCatalog(searchTerm, category, targetAudience, collectionId,
				minPrice, maxPrice, isFeatured, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> update(@PathVariable Long id,
			@RequestBody @Valid ProductRequestDTO request) {
		return ResponseEntity.ok(productService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
