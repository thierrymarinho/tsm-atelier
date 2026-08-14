package com.tm.tsm_atelier.domain.admin.controller.v1;

import com.tm.tsm_atelier.domain.admin.dto.DashboardResponseDTO;
import com.tm.tsm_atelier.domain.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	/**
	 * O limiar de estoque baixo é parâmetro porque ele depende do giro da loja: 5 é
	 * um palpite razoável para peça de vestuário e não serve para todo mundo.
	 *
	 *
	 * lowStockPage existe porque a amostra de vinte linhas era um beco sem saída: a
	 * resposta dizia "20 de 37" e não havia como chegar aos outros dezessete. Como
	 * a lista vem ordenada do menor estoque para o maior, mexer no limiar corta
	 * pelo lado errado — os que faltam são justamente os de estoque mais alto, e
	 * não existe piso para excluir os já vistos.
	 *
	 *
	 * Aditivo e com default: quem já chamava este endpoint sem o parâmetro continua
	 * recebendo a primeira página, como antes.
	 */
	@GetMapping
	public ResponseEntity<DashboardResponseDTO> summary(@RequestParam(defaultValue = "5") int lowStockThreshold,
			@RequestParam(defaultValue = "0") int lowStockPage) {
		return ResponseEntity.ok(dashboardService.summary(lowStockThreshold, lowStockPage));
	}
}
