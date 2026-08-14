package com.tm.tsm_atelier.domain.order.repository;

import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository
		extends
			JpaRepository<Order, Long>,
			org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
	org.springframework.data.domain.Page<Order> findByUserId(UUID userId,
			org.springframework.data.domain.Pageable pageable);

	List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime date);

	java.util.Optional<Order> findByPaymentIntentId(String paymentIntentId);

	/**
	 * A listagem do admin traz o usuário junto porque o DTO de admin identifica o
	 * cliente. Sem o entity graph seria uma ida ao banco por pedido só para ler
	 * nome e e-mail — o batch fetch reduziria, mas não elimina um N+1 que um join
	 * resolve de graça, já que a associação é ManyToOne e não interfere na
	 * paginação.
	 *
	 * <p>
	 * A redeclaração existe só para pendurar o entity graph: a assinatura herdada
	 * de {@link JpaSpecificationExecutor} não tem onde recebê-lo.
	 */
	@Override
	@org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
	org.springframework.data.domain.Page<Order> findAll(org.springframework.data.jpa.domain.Specification<Order> spec,
			org.springframework.data.domain.Pageable pageable);

	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
	java.util.Optional<Order> findByIdWithItems(@Param("id") Long id);

	@Query("SELECT o.id FROM Order o WHERE o.status IN :statuses AND o.expiresAt < :now ORDER BY o.id")
	List<Long> findIdsByStatusInAndExpiresAtBefore(@Param("statuses") Collection<OrderStatus> statuses,
			@Param("now") LocalDateTime now);

	/**
	 * As duas consultas do dashboard agregam no banco. Contar e somar carregando os
	 * pedidos funcionaria com o volume de hoje e passaria a trazer a tabela inteira
	 * para a memória exatamente quando a loja crescesse — que é quando alguém abre
	 * um painel.
	 */
	@Query("""
			SELECT new com.tm.tsm_atelier.domain.order.repository.OrderStatusCount(o.status, COUNT(o))
			FROM Order o GROUP BY o.status
			""")
	List<OrderStatusCount> countByStatus();

	/**
	 * O COALESCE não é enfeite: sem nenhum pedido no período o SUM devolve NULL, e
	 * o dashboard mostraria um campo vazio onde deveria mostrar zero.
	 */
	@Query("""
			SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
			WHERE o.status IN :statuses AND o.createdAt >= :since
			""")
	java.math.BigDecimal sumTotalAmountSince(@Param("statuses") Collection<OrderStatus> statuses,
			@Param("since") LocalDateTime since);
}
