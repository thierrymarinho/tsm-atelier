package com.tm.tsm_atelier.infrastructure.scheduler;

import com.tm.tsm_atelier.domain.order.service.OrderService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduler.order-expiration.enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {

	private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

	private final OrderService orderService;

	public OrderExpirationScheduler(OrderService orderService) {
		this.orderService = orderService;
	}

	// Runs every minute
	@Scheduled(cron = "0 * * * * *")
	public void cancelExpiredOrders() {
		List<Long> expiredOrderIds = orderService.findExpiredOrderIds();
		if (expiredOrderIds.isEmpty()) {
			return;
		}

		log.info("Found {} expired orders to cancel", expiredOrderIds.size());

		int cancelled = 0;
		for (Long orderId : expiredOrderIds) {
			try {
				if (orderService.cancelAndRestoreStock(orderId)) {
					cancelled++;
				}
			} catch (OptimisticLockingFailureException e) {
				log.info("Order {} was updated concurrently; skipping cancellation", orderId);
			} catch (Exception e) {
				log.error("Failed to cancel expired order {}", orderId, e);
			}
		}

		log.info("Cancelled {} of {} expired orders and returned their stock", cancelled, expiredOrderIds.size());
	}
}
