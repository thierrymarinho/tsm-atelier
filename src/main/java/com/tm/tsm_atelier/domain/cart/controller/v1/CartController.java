package com.tm.tsm_atelier.domain.cart.controller.v1;

import com.tm.tsm_atelier.domain.cart.dto.CartItemRequestDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartItemUpdateDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartResponseDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartSyncRequestDTO;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<CartResponseDTO> getCart(@AuthenticationPrincipal User user) {
		return ResponseEntity.ok(cartService.getCart(user.getId()));
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponseDTO> addItem(@AuthenticationPrincipal User user,
			@Valid @RequestBody CartItemRequestDTO request) {
		return ResponseEntity.ok(cartService.addItem(user.getId(), request));
	}

	@PutMapping("/items/{itemId}")
	public ResponseEntity<CartResponseDTO> updateItemQuantity(@AuthenticationPrincipal User user,
			@PathVariable Long itemId, @Valid @RequestBody CartItemUpdateDTO request) {
		return ResponseEntity.ok(cartService.updateItemQuantity(user.getId(), itemId, request.quantity()));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<CartResponseDTO> removeItem(@AuthenticationPrincipal User user, @PathVariable Long itemId) {
		return ResponseEntity.ok(cartService.removeItem(user.getId(), itemId));
	}

	@PostMapping("/sync")
	public ResponseEntity<CartResponseDTO> syncCart(@AuthenticationPrincipal User user,
			@Valid @RequestBody CartSyncRequestDTO request) {
		return ResponseEntity.ok(cartService.syncCart(user.getId(), request));
	}

	@DeleteMapping
	public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
		cartService.clearCart(user.getId());
		return ResponseEntity.noContent().build();
	}
}
