package com.tm.tsm_atelier.domain.admin.service;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.domain.admin.dto.DashboardResponseDTO;
import com.tm.tsm_atelier.domain.admin.dto.LowStockSkuDTO;
import com.tm.tsm_atelier.domain.admin.dto.RevenueDTO;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.order.repository.OrderStatusCount;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

	/**
	 * O que conta como faturamento. PENDING_PAYMENT ainda não é dinheiro,
	 * PAYMENT_FAILED nunca foi, e CANCELLED sai da conta.
	 */
	private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(OrderStatus.PAID, OrderStatus.SHIPPED,
			OrderStatus.DELIVERED);

	/**
	 * A lista de estoque baixo é um alerta, e não um relatório: vinte linhas cabem
	 * na tela e o total acompanha à parte, para a interface poder dizer "20 de 47".
	 */
	private static final int LOW_STOCK_SAMPLE_SIZE = 20;

	private static final int MAX_LOW_STOCK_THRESHOLD = 1_000;

	private final OrderRepository orderRepository;
	private final ProductSKURepository skuRepository;

	/**
	 * A primeira página do estoque baixo — o comportamento que este método sempre
	 * teve. Mantido como sobrecarga para que a paginação não obrigue todo chamador
	 * a declarar um zero que não lhe diz respeito.
	 */
	@Transactional(readOnly = true)
	public DashboardResponseDTO summary(int lowStockThreshold) {
		return summary(lowStockThreshold, 0);
	}

	@Transactional(readOnly = true)
	public DashboardResponseDTO summary(int lowStockThreshold, int lowStockPage) {
		if (lowStockThreshold < 0) {
			throw new BusinessRuleException("lowStockThreshold cannot be negative.");
		}

		// Sem esta guarda o PageRequest lança IllegalArgumentException, que sobe
		// como 500 — um erro de quem chamou apresentado como falha do servidor.
		if (lowStockPage < 0) {
			throw new BusinessRuleException("lowStockPage cannot be negative.");
		}

		// Sem teto, um limiar absurdo faria o "alerta" devolver o catálogo inteiro
		// ordenado por estoque — caro, e sem significar nada.
		if (lowStockThreshold > MAX_LOW_STOCK_THRESHOLD) {
			throw new BusinessRuleException("lowStockThreshold cannot exceed " + MAX_LOW_STOCK_THRESHOLD + " (received "
					+ lowStockThreshold + ").");
		}

		LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

		return new DashboardResponseDTO(ordersByStatus(),
				new RevenueDTO(revenueSince(startOfToday), revenueSince(startOfToday.minusDays(6)),
						revenueSince(startOfToday.minusDays(29))),
				lowStock(lowStockThreshold, lowStockPage), skuRepository.countLowStock(lowStockThreshold),
				LOW_STOCK_SAMPLE_SIZE, lowStockPage);
	}

	/**
	 * Todos os status aparecem, inclusive os zerados. O agrupamento do banco só
	 * devolve linhas que existem, e um mapa incompleto obrigaria a interface a
	 * tratar "nenhum pedido cancelado" e "chave ausente" como o mesmo caso — que é
	 * como um contador some da tela sem ninguém perceber.
	 */
	private Map<OrderStatus, Long> ordersByStatus() {
		Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
		for (OrderStatus status : OrderStatus.values()) {
			counts.put(status, 0L);
		}

		for (OrderStatusCount row : orderRepository.countByStatus()) {
			counts.put(row.status(), row.total());
		}

		return counts;
	}

	/**
	 * As janelas contam dias inteiros a partir da meia-noite: "últimos 7 dias" com
	 * {@code now().minusDays(7)} produziria um número que muda de significado ao
	 * longo do dia, e dois acessos à mesma tela mostrariam totais diferentes sem
	 * nenhuma venda ter acontecido.
	 */
	private java.math.BigDecimal revenueSince(LocalDateTime since) {
		return orderRepository.sumTotalAmountSince(REVENUE_STATUSES, since);
	}

	private List<LowStockSkuDTO> lowStock(int threshold, int page) {
		return skuRepository.findLowStock(threshold, PageRequest.of(page, LOW_STOCK_SAMPLE_SIZE)).stream()
				.map(this::toLowStockDTO).toList();
	}

	private LowStockSkuDTO toLowStockDTO(ProductSKU sku) {
		var color = sku.getProductColor();
		var product = color.getProduct();

		return new LowStockSkuDTO(sku.getId(), sku.getSkuCode(), product.getId(), product.getName(),
				color.getColorName(), sku.getSize(), sku.getStockQuantity(), sku.getVersion());
	}
}
