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
public interface OrderRepository extends JpaRepository<Order, Long> {
	org.springframework.data.domain.Page<Order> findByUserId(UUID userId,
			org.springframework.data.domain.Pageable pageable);

	List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime date);

	java.util.Optional<Order> findByPaymentIntentId(String paymentIntentId);

	org.springframework.data.domain.Page<Order> findByStatus(OrderStatus status,
			org.springframework.data.domain.Pageable pageable);

	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
	java.util.Optional<Order> findByIdWithItems(@Param("id") Long id);

	@Query("SELECT o.id FROM Order o WHERE o.status IN :statuses AND o.expiresAt < :now ORDER BY o.id")
	List<Long> findIdsByStatusInAndExpiresAtBefore(@Param("statuses") Collection<OrderStatus> statuses,
			@Param("now") LocalDateTime now);
}
