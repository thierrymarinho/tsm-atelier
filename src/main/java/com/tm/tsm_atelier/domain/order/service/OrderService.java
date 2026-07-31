package com.tm.tsm_atelier.domain.order.service;

import com.tm.tsm_atelier.common.exception.custom.AddressNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.dto.CheckoutItemDTO;
import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderItemResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderItem;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.event.OrderPaidEvent;
import com.tm.tsm_atelier.domain.order.port.PaymentIntentResult;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.Address;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.AddressRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the transactional units of the order lifecycle. Every public method here
 * is a single, short database transaction — calls to external systems (payment
 * gateway, e-mail) are deliberately kept out, and orchestrated by
 * {@link CheckoutService} and the expiration scheduler instead.
 */
@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final ProductSKURepository skuRepository;
	private final AddressRepository addressRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final CartService cartService;

	private static final BigDecimal FIXED_SHIPPING_FEE = new BigDecimal("0.00");
	private static final int PAYMENT_WINDOW_MINUTES = 30;

	/**
	 * Statuses where the order still holds reserved stock and a payment is still
	 * expected: the only ones from which it may be cancelled with a stock refund,
	 * and the only ones a successful payment may transition to PAID.
	 */
	private static final Set<OrderStatus> AWAITING_PAYMENT_STATUSES = Set.of(OrderStatus.PENDING_PAYMENT,
			OrderStatus.PAYMENT_FAILED);

	public OrderService(OrderRepository orderRepository, ProductSKURepository skuRepository,
			AddressRepository addressRepository, ApplicationEventPublisher eventPublisher, CartService cartService) {
		this.orderRepository = orderRepository;
		this.skuRepository = skuRepository;
		this.addressRepository = addressRepository;
		this.eventPublisher = eventPublisher;
		this.cartService = cartService;
	}

	/**
	 * Reserves stock and persists the order as PENDING_PAYMENT. Commits before any
	 * payment gateway call so the pessimistic SKU locks are not held across the
	 * network.
	 */
	@Transactional
	public Order createPendingOrder(User user, CheckoutRequestDTO request) {
		Address address = addressRepository.findById(request.addressId())
				.orElseThrow(() -> new AddressNotFoundException("Address not found."));

		if (!address.getUser().getId().equals(user.getId())) {
			throw new AddressNotFoundException("Address not found.");
		}

		ShippingAddress shippingAddress = ShippingAddress.builder().street(address.getStreet())
				.number(address.getNumber()).complement(address.getComplement()).neighborhood(address.getNeighborhood())
				.city(address.getCity()).state(address.getState()).zipCode(address.getZipCode()).build();

		Order order = Order.builder().user(user).status(OrderStatus.PENDING_PAYMENT).shippingAddress(shippingAddress)
				.shippingFee(FIXED_SHIPPING_FEE).expiresAt(LocalDateTime.now().plusMinutes(PAYMENT_WINDOW_MINUTES))
				.build();

		BigDecimal totalAmount = BigDecimal.ZERO;

		for (Map.Entry<Long, Integer> requestedItem : consolidateItems(request.items()).entrySet()) {
			Long skuId = requestedItem.getKey();
			int quantity = requestedItem.getValue();

			ProductSKU sku = skuRepository.findByIdWithPessimisticLock(skuId)
					.orElseThrow(() -> new ResourceNotFoundException("SKU", skuId));

			if (sku.getStockQuantity() < quantity) {
				throw new OutOfStockException(
						"Out of stock for SKU: " + sku.getSkuCode() + ". Available: " + sku.getStockQuantity());
			}

			sku.setStockQuantity(sku.getStockQuantity() - quantity);

			BigDecimal priceAtPurchase = sku.getProductColor().getProduct().getPrice();

			OrderItem orderItem = OrderItem.builder().sku(sku).productName(sku.getProductColor().getProduct().getName())
					.skuCode(sku.getSkuCode()).size(sku.getSize().name()).color(sku.getProductColor().getColorName())
					.imageUrl(sku.getProductColor().getCoverImageUrl()).priceAtPurchase(priceAtPurchase)
					.quantity(quantity).build();

			order.addItem(orderItem);

			totalAmount = totalAmount.add(priceAtPurchase.multiply(new BigDecimal(quantity)));
		}

		order.setTotalAmount(totalAmount.add(FIXED_SHIPPING_FEE));

		Order savedOrder = orderRepository.save(order);

		// Limpa o carrinho após a criação do pedido
		cartService.clearCart(user.getId());

		return savedOrder;
	}

	/**
	 * Merges duplicate SKUs and returns them ordered by id, so concurrent checkouts
	 * always acquire the pessimistic SKU locks in the same order and cannot
	 * deadlock each other.
	 */
	private static SortedMap<Long, Integer> consolidateItems(List<CheckoutItemDTO> items) {
		SortedMap<Long, Integer> quantityBySkuId = new TreeMap<>();
		for (CheckoutItemDTO item : items) {
			quantityBySkuId.merge(item.skuId(), item.quantity(), Integer::sum);
		}
		return quantityBySkuId;
	}

	/** Second half of checkout: links the created PaymentIntent to the order. */
	@Transactional
	public OrderResponseDTO attachPaymentIntent(Long orderId, PaymentIntentResult paymentIntentResult) {
		Order order = orderRepository.findByIdWithItems(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		order.setPaymentIntentId(paymentIntentResult.paymentIntentId());

		return toResponseDTO(order, paymentIntentResult.clientSecret());
	}

	/**
	 * Cancels an order that never got paid and returns its reserved stock. Returns
	 * {@code false} without touching stock when the order already moved on (e.g. it
	 * was paid in the meantime).
	 */
	@Transactional
	public boolean cancelAndRestoreStock(Long orderId) {
		Order order = orderRepository.findByIdWithItems(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		if (!AWAITING_PAYMENT_STATUSES.contains(order.getStatus())) {
			log.debug("Order {} is {}; skipping cancellation", orderId, order.getStatus());
			return false;
		}

		order.setStatus(OrderStatus.CANCELLED);

		for (OrderItem item : order.getItems()) {
			if (item.getSku() == null) {
				continue;
			}
			skuRepository.findByIdWithPessimisticLock(item.getSku().getId())
					.ifPresent(sku -> sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity()));
		}

		return true;
	}

	/**
	 * Ids of orders whose payment window has closed, for the scheduler to cancel.
	 */
	@Transactional(readOnly = true)
	public List<Long> findExpiredOrderIds() {
		return orderRepository.findIdsByStatusInAndExpiresAtBefore(AWAITING_PAYMENT_STATUSES, LocalDateTime.now());
	}

	@Transactional
	public void handlePaymentSuccess(String paymentIntentId) {
		Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
				.orElseThrow(() -> new ResourceNotFoundException("Order for paymentIntent", paymentIntentId));

		if (order.getStatus() == OrderStatus.PAID) {
			log.info("Order {} is already PAID; ignoring duplicate webhook delivery", order.getId());
			return;
		}

		if (!AWAITING_PAYMENT_STATUSES.contains(order.getStatus())) {
			// Money was captured for an order we can no longer fulfil as-is (most likely
			// cancelled by expiration). Needs manual reconciliation / refund.
			log.error("Payment succeeded for order {} in unexpected status {} — manual review required", order.getId(),
					order.getStatus());
			return;
		}

		order.setStatus(OrderStatus.PAID);

		// Sent only after this transaction commits, so a mail failure cannot roll the
		// PAID status back.
		eventPublisher.publishEvent(new OrderPaidEvent(order.getId(), order.getUser().getEmail(),
				order.getUser().getFirstName(), order.getTotalAmount()));
	}

	@Transactional
	public void handlePaymentFailure(String paymentIntentId) {
		Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
				.orElseThrow(() -> new ResourceNotFoundException("Order for paymentIntent", paymentIntentId));

		if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			order.setStatus(OrderStatus.PAYMENT_FAILED);
		}
	}

	@Transactional(readOnly = true)
	public Page<OrderResponseDTO> getMyOrders(User user, Pageable pageable) {
		return orderRepository.findByUserId(user.getId(), pageable).map(order -> toResponseDTO(order, null));
	}

	@Transactional(readOnly = true)
	public OrderResponseDTO getOrderDetails(Long id, User user) {
		Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));

		if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
			throw new IllegalArgumentException("Access denied");
		}

		return toResponseDTO(order, null);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable) {
		if (status != null) {
			return orderRepository.findByStatus(status, pageable).map(order -> toResponseDTO(order, null));
		}
		return orderRepository.findAll(pageable).map(order -> toResponseDTO(order, null));
	}

	@Transactional
	public OrderResponseDTO updateOrderStatus(Long id, OrderStatus newStatus) {
		Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
		order.setStatus(newStatus);
		return toResponseDTO(order, null);
	}

	private OrderResponseDTO toResponseDTO(Order order, String clientSecret) {
		List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
				.map(item -> new OrderItemResponseDTO(item.getId(),
						item.getSku() != null ? item.getSku().getId() : null, item.getProductName(), item.getSkuCode(),
						item.getSize(), item.getColor(), item.getImageUrl(), item.getPriceAtPurchase(),
						item.getQuantity()))
				.toList();

		return new OrderResponseDTO(order.getId(), order.getStatus(), order.getTotalAmount(), order.getShippingFee(),
				clientSecret, order.getShippingAddress(), order.getExpiresAt(), order.getCreatedAt(), itemDTOs);
	}
}
