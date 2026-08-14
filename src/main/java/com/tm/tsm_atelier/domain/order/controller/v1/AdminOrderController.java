package com.tm.tsm_atelier.domain.order.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.order.dto.AdminOrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderSearchFilter;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.service.OrderService;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

	/**
	 * Sem esta lista, {@code ?sort=user.password} era aceito: o Spring Data resolve
	 * caminhos aninhados e o @PageableDefault só define o padrão. Ordenar por campo
	 * sensível é vetor de inferência, e uma propriedade inexistente virava 500.
	 */
	private static final Set<String> SORTABLE_FIELDS = Set.of("id", "status", "totalAmount", "createdAt", "updatedAt",
			"expiresAt");

	private final OrderService orderService;

	public AdminOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ResponseEntity<Page<AdminOrderResponseDTO>> getAllOrders(@ModelAttribute OrderSearchFilter filter,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(orderService.getAllOrders(filter, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AdminOrderResponseDTO> getOrderDetails(@PathVariable Long id) {
		return ResponseEntity.ok(orderService.getAdminOrderDetails(id));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<AdminOrderResponseDTO> updateOrderStatus(@PathVariable Long id,
			@RequestParam OrderStatus newStatus) {
		return ResponseEntity.ok(orderService.updateOrderStatus(id, newStatus));
	}
}
