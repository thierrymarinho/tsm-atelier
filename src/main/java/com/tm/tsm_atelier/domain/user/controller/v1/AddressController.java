package com.tm.tsm_atelier.domain.user.controller.v1;

import com.tm.tsm_atelier.domain.user.dto.AddressRequestDTO;
import com.tm.tsm_atelier.domain.user.dto.AddressResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

	private final AddressService addressService;

	public AddressController(AddressService addressService) {
		this.addressService = addressService;
	}

	@GetMapping
	public ResponseEntity<List<AddressResponseDTO>> getAllAddresses(@AuthenticationPrincipal User user) {
		return ResponseEntity.ok(addressService.findAllByUser(user));
	}

	@PostMapping
	public ResponseEntity<AddressResponseDTO> createAddress(@AuthenticationPrincipal User user,
			@Valid @RequestBody AddressRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(user, request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AddressResponseDTO> updateAddress(@AuthenticationPrincipal User user, @PathVariable Long id,
			@Valid @RequestBody AddressRequestDTO request) {
		return ResponseEntity.ok(addressService.update(user, id, request));
	}

	@PatchMapping("/{id}/default")
	public ResponseEntity<AddressResponseDTO> setDefaultAddress(@AuthenticationPrincipal User user,
			@PathVariable Long id) {
		return ResponseEntity.ok(addressService.setDefault(user, id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal User user, @PathVariable Long id) {
		addressService.delete(user, id);
		return ResponseEntity.noContent().build();
	}
}
