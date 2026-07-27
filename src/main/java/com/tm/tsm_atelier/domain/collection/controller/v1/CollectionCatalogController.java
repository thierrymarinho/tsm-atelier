package com.tm.tsm_atelier.domain.collection.controller.v1;

import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.service.CollectionService;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
		Long id = extractIdFromSlug(slug);

		CollectionResponseDTO collection = collectionService.findById(id);

		if (!collection.slug().equals(slug)) {
			return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
					.header("Location", "/api/v1/catalog/collections/slug/" + collection.slug()).build();
		}

		return ResponseEntity.ok(collection);
	}

	private Long extractIdFromSlug(String slug) {
		try {
			String[] parts = slug.split("-");
			return Long.parseLong(parts[parts.length - 1]);
		} catch (NumberFormatException e) {
			throw new ResourceNotFoundException("Collection", slug);
		}
	}
}
