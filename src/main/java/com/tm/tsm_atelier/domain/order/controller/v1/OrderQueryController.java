package com.tm.tsm_atelier.domain.order.controller.v1;

import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.service.OrderService;
import com.tm.tsm_atelier.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {

	private final OrderService orderService;

	public OrderQueryController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/my-orders")
	public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(@AuthenticationPrincipal User user,
			@PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(orderService.getMyOrders(user, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderResponseDTO> getOrderDetails(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return ResponseEntity.ok(orderService.getOrderDetails(id, user));
	}
}
