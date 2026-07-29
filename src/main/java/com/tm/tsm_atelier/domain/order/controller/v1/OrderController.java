package com.tm.tsm_atelier.domain.order.controller.v1;

import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.service.CheckoutService;
import com.tm.tsm_atelier.domain.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final CheckoutService checkoutService;

	public OrderController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/checkout")
	public ResponseEntity<OrderResponseDTO> checkout(@AuthenticationPrincipal User user,
			@Valid @RequestBody CheckoutRequestDTO request) {
		OrderResponseDTO response = checkoutService.checkout(user, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
