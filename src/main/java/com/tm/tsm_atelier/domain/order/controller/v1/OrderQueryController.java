package com.tm.tsm_atelier.domain.order.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.service.OrderService;
import com.tm.tsm_atelier.domain.user.entity.User;
import java.util.Set;
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

	/**
	 * O que o cliente pode ordenar nos proprios pedidos. Sem esta lista o Spring
	 * Data resolvia caminho aninhado a partir da query string, e
	 * ?sort=user.password era aceito com 200: ordenar por um campo nao revela o
	 * valor, mas e vetor conhecido de inferencia sobre dado sensivel — o mesmo que
	 * o javadoc de SortWhitelist descreve e que as rotas de admin ja barravam.
	 * Esta, que recebe input de usuario comum, era a que estava aberta.
	 */
	private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "totalAmount", "status");

	private final OrderService orderService;

	public OrderQueryController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/my-orders")
	public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(@AuthenticationPrincipal User user,
			@PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(orderService.getMyOrders(user, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderResponseDTO> getOrderDetails(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return ResponseEntity.ok(orderService.getOrderDetails(id, user));
	}
}
