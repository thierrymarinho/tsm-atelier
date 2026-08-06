package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
	public ResponseEntity<Page<ProductSummaryDTO>> search(@ModelAttribute ProductSearchFilter filter,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(productService.searchAdmin(filter, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findAdminById(id));
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
