package com.tm.tsm_atelier.domain.collection.controller.v1;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.service.CollectionService;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/collections")
@RequiredArgsConstructor
public class CollectionCatalogController {

	private final CollectionService collectionService;

	@GetMapping
	public ResponseEntity<List<CollectionResponseDTO>> findAll(@RequestParam(required = false) DisplayPosition position,
			@RequestParam(required = false) TargetAudience targetAudience) {

		return ResponseEntity.ok(collectionService.findByFilters(position, targetAudience));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CollectionResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(collectionService.findById(id));
	}

	@GetMapping("/slug/{slug}")
	public ResponseEntity<CollectionResponseDTO> findBySlug(@PathVariable String slug) {
		return ResponseEntity.ok(collectionService.findBySlug(slug));
	}
}
