package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.product.dto.CareAxisOptionsDTO;
import com.tm.tsm_atelier.domain.product.dto.MaterialOptionDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class ProductCatalogController {

	private static final Set<String> SORTABLE_FIELDS = Set.of("name", "price", "promotionalPrice", "createdAt");

	private final ProductService productService;

	@GetMapping
	public ResponseEntity<Page<ProductSummaryDTO>> search(@ModelAttribute ProductSearchFilter filter,
			@PageableDefault(size = 12) Pageable pageable) {

		return ResponseEntity
				.ok(productService.searchCatalog(filter, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}

	@GetMapping("/slug/{slug}")
	public ResponseEntity<ProductResponseDTO> findBySlug(@PathVariable String slug) {
		return ResponseEntity.ok(productService.findBySlug(slug));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findById(id));
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getCategories(@RequestParam(required = false) TargetAudience targetAudience) {
		return ResponseEntity.ok(Category.getByTargetAudience(targetAudience));
	}

	@GetMapping("/materials")
	public ResponseEntity<List<MaterialOptionDTO>> getMaterials() {
		return ResponseEntity.ok(Arrays.stream(Material.values()).map(MaterialOptionDTO::from).toList());
	}

	@GetMapping("/care-instructions")
	public ResponseEntity<List<CareAxisOptionsDTO>> getCareInstructions() {
		return ResponseEntity.ok(Arrays.stream(CareAxis.values()).map(CareAxisOptionsDTO::from).toList());
	}
}
