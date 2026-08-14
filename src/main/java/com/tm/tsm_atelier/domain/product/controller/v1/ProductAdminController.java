package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.AdminProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.Set;
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

	private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "promotionalPrice", "category",
			"targetAudience", "active", "featured", "createdAt", "updatedAt");

	private final ProductService productService;

	@PostMapping
	public ResponseEntity<AdminProductResponseDTO> create(@RequestBody @Valid ProductRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
	}

	@GetMapping
	public ResponseEntity<Page<AdminProductSummaryDTO>> search(@ModelAttribute ProductSearchFilter filter,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(productService.searchAdmin(filter, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AdminProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findAdminById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AdminProductResponseDTO> update(@PathVariable Long id,
			@RequestBody @Valid ProductRequestDTO request) {
		return ResponseEntity.ok(productService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/restore")
	public ResponseEntity<AdminProductResponseDTO> restore(@PathVariable Long id) {
		return ResponseEntity.ok(productService.restore(id));
	}
}
