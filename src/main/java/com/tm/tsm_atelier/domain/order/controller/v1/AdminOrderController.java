package com.tm.tsm_atelier.domain.order.controller.v1;

import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

	private final OrderService orderService;

	public AdminOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(@RequestParam(required = false) OrderStatus status,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<OrderResponseDTO> updateOrderStatus(@PathVariable Long id,
			@RequestParam OrderStatus newStatus) {
		return ResponseEntity.ok(orderService.updateOrderStatus(id, newStatus));
	}
}
