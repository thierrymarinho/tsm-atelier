package com.tm.tsm_atelier.domain.collection.controller.v1;

import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.service.CollectionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
public class CollectionController {

	private final CollectionService collectionService;

	@PostMapping
	public ResponseEntity<CollectionResponseDTO> create(@RequestBody @Valid CollectionRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.create(request));
	}

	@GetMapping
	public ResponseEntity<List<CollectionResponseDTO>> findAll() {
		return ResponseEntity.ok(collectionService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CollectionResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(collectionService.findById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CollectionResponseDTO> update(@PathVariable Long id,
			@RequestBody @Valid CollectionRequestDTO request) {
		return ResponseEntity.ok(collectionService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		collectionService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
