package com.tm.tsm_atelier.domain.order.service;

import com.tm.tsm_atelier.common.exception.custom.AddressNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.InvalidStatusTransitionException;
import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.dto.AdminOrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.CheckoutItemDTO;
import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderItemResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderSearchFilter;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderItem;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.event.OrderPaidEvent;
import com.tm.tsm_atelier.domain.order.port.PaymentIntentResult;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.order.repository.OrderSpecification;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.Address;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final ProductSKURepository skuRepository;
	private final AddressRepository addressRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final CartService cartService;
	private final AuditService auditService;

	private static final BigDecimal FIXED_SHIPPING_FEE = new BigDecimal("0.00");
	private static final int PAYMENT_WINDOW_MINUTES = 30;

	private static final Set<OrderStatus> AWAITING_PAYMENT_STATUSES = Set.of(OrderStatus.PENDING_PAYMENT,
			OrderStatus.PAYMENT_FAILED);

	/**
	 * Transições permitidas na atualização manual de status. Sem esse mapa a rota
	 * de admin aceitava qualquer destino, inclusive voltar um pedido entregue para
	 * aguardando pagamento ou reabrir um cancelado.
	 */
	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(OrderStatus.PENDING_PAYMENT,
			Set.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED), OrderStatus.PAYMENT_FAILED,
			Set.of(OrderStatus.PAID, OrderStatus.CANCELLED), OrderStatus.PAID,
			Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED), OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
			OrderStatus.DELIVERED, Set.of(), OrderStatus.CANCELLED, Set.of());

	public OrderService(OrderRepository orderRepository, ProductSKURepository skuRepository,
			AddressRepository addressRepository, ApplicationEventPublisher eventPublisher, CartService cartService,
			AuditService auditService) {
		this.orderRepository = orderRepository;
		this.skuRepository = skuRepository;
		this.addressRepository = addressRepository;
		this.eventPublisher = eventPublisher;
		this.cartService = cartService;
		this.auditService = auditService;
	}

	@Transactional
	@CacheEvict(value = CacheNames.CATALOG_SLUG, allEntries = true)
	public Order createPendingOrder(User user, CheckoutRequestDTO request) {
		Address address = addressRepository.findById(request.addressId())
				.orElseThrow(() -> new AddressNotFoundException("Address not found."));

		if (!address.getUser().getId().equals(user.getId())) {
			throw new AddressNotFoundException("Address not found.");
		}

		ShippingAddress shippingAddress = ShippingAddress.builder().street(address.getStreet())
				.number(address.getNumber()).complement(address.getComplement()).neighborhood(address.getNeighborhood())
				.city(address.getCity()).state(address.getState().name()).postalCode(address.getPostalCode()).build();

		Order order = Order.builder().user(user).status(OrderStatus.PENDING_PAYMENT).shippingAddress(shippingAddress)
				.shippingFee(FIXED_SHIPPING_FEE).expiresAt(LocalDateTime.now().plusMinutes(PAYMENT_WINDOW_MINUTES))
				.build();

		BigDecimal totalAmount = BigDecimal.ZERO;

		for (Map.Entry<Long, Integer> requestedItem : consolidateItems(request.items()).entrySet()) {
			Long skuId = requestedItem.getKey();
			int quantity = requestedItem.getValue();

			ProductSKU sku = skuRepository.findByIdWithPessimisticLock(skuId)
					.orElseThrow(() -> new ResourceNotFoundException("SKU", skuId));

			if (!sku.getProductColor().getProduct().isActive()) {
				throw new OutOfStockException("This product is no longer available.", 0);
			}

			if (sku.getStockQuantity() < quantity) {
				throw new OutOfStockException(
						"Out of stock for SKU: " + sku.getSkuCode() + ". Available: " + sku.getStockQuantity(),
						sku.getStockQuantity());
			}

			sku.setStockQuantity(sku.getStockQuantity() - quantity);

			BigDecimal priceAtPurchase = sku.getProductColor().getProduct().getEffectivePrice();

			OrderItem orderItem = OrderItem.builder().sku(sku).productName(sku.getProductColor().getProduct().getName())
					.skuCode(sku.getSkuCode()).size(sku.getSize().name()).color(sku.getProductColor().getColorName())
					.imageUrl(sku.getProductColor().getCoverImageUrl()).priceAtPurchase(priceAtPurchase)
					.listPriceAtPurchase(sku.getProductColor().getProduct().getPrice()).quantity(quantity).build();

			order.addItem(orderItem);

			totalAmount = totalAmount.add(priceAtPurchase.multiply(new BigDecimal(quantity)));
		}

		order.setTotalAmount(totalAmount.add(FIXED_SHIPPING_FEE));

		Order savedOrder = orderRepository.save(order);

		cartService.clearCart(user.getId());

		return savedOrder;
	}

	private static SortedMap<Long, Integer> consolidateItems(List<CheckoutItemDTO> items) {
		SortedMap<Long, Integer> quantityBySkuId = new TreeMap<>();
		for (CheckoutItemDTO item : items) {
			quantityBySkuId.merge(item.skuId(), item.quantity(), Integer::sum);
		}
		return quantityBySkuId;
	}

	@Transactional
	public OrderResponseDTO attachPaymentIntent(Long orderId, PaymentIntentResult paymentIntentResult) {
		Order order = orderRepository.findByIdWithItems(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		order.setPaymentIntentId(paymentIntentResult.paymentIntentId());
		order.setPaymentClientSecret(paymentIntentResult.clientSecret());

		return toResponseDTO(order, paymentIntentResult.clientSecret());
	}

	@Transactional
	@CacheEvict(value = CacheNames.CATALOG_SLUG, allEntries = true)
	public boolean cancelAndRestoreStock(Long orderId) {
		Order order = orderRepository.findByIdWithItems(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		if (!AWAITING_PAYMENT_STATUSES.contains(order.getStatus())) {
			log.debug("Order {} is {}; skipping cancellation", orderId, order.getStatus());
			return false;
		}

		order.setStatus(OrderStatus.CANCELLED);
		restoreStock(order);

		return true;
	}

	/**
	 * Devolve ao estoque as unidades que o pedido mantinha reservadas. Pega o lock
	 * pessimista no SKU pelo mesmo motivo do checkout: duas devoluções simultâneas
	 * sobre o mesmo SKU perderiam uma das somas.
	 */
	private void restoreStock(Order order) {
		for (OrderItem item : order.getItems()) {
			if (item.getSku() == null) {
				continue;
			}
			skuRepository.findByIdWithPessimisticLock(item.getSku().getId()).ifPresentOrElse(
					sku -> sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity()),
					() -> log.warn("SKU {} ({}) is no longer in the catalog; {} units from order {} were not restored",
							item.getSku().getId(), item.getSkuCode(), item.getQuantity(), order.getId()));
		}
	}

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
			log.error("Payment succeeded for order {} in unexpected status {} — manual review required", order.getId(),
					order.getStatus());
			return;
		}

		order.setStatus(OrderStatus.PAID);

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
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));

		if (!order.getUser().getId().equals(user.getId())) {
			throw new AccessDeniedException("Access denied");
		}

		return toResponseDTO(order, null);
	}

	@Transactional(readOnly = true)
	public AdminOrderResponseDTO getAdminOrderDetails(Long id) {
		return toAdminResponseDTO(
				orderRepository.findByIdWithItems(id).orElseThrow(() -> new ResourceNotFoundException("Order", id)));
	}

	@Transactional(readOnly = true)
	public Page<AdminOrderResponseDTO> getAllOrders(OrderSearchFilter filter, Pageable pageable) {
		if (filter.createdFrom() != null && filter.createdTo() != null
				&& filter.createdFrom().isAfter(filter.createdTo())) {
			throw new BusinessRuleException(
					"createdFrom (" + filter.createdFrom() + ") is after createdTo (" + filter.createdTo() + ").");
		}

		Specification<Order> spec = Specification.where(OrderSpecification.hasStatus(filter.status()))
				.and(OrderSpecification.search(filter.searchTerm()))
				.and(OrderSpecification.createdFrom(filter.createdFrom()))
				.and(OrderSpecification.createdTo(filter.createdTo()));

		return orderRepository.findAll(spec, pageable).map(this::toAdminResponseDTO);
	}

	@Transactional
	public AdminOrderResponseDTO updateOrderStatus(Long id, OrderStatus newStatus) {
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));

		OrderStatus previousStatus = order.getStatus();

		if (previousStatus == newStatus) {
			return toAdminResponseDTO(order);
		}

		if (!ALLOWED_TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(newStatus)) {
			throw new InvalidStatusTransitionException(previousStatus, newStatus);
		}

		if (newStatus == OrderStatus.CANCELLED) {
			restoreStock(order);
		}

		order.setStatus(newStatus);

		auditService.recordChange(AuditedEntity.ORDER, id, AuditAction.STATUS_CHANGED, previousStatus, newStatus);

		return toAdminResponseDTO(order);
	}

	/**
	 * Este mapeamento serve exclusivamente o dono do pedido, e por isso pode
	 * carregar o clientSecret sem condicional nenhuma: quem chega aqui já passou
	 * pela checagem de posse.
	 */
	private OrderResponseDTO toResponseDTO(Order order, String clientSecret) {
		String resolvedClientSecret = clientSecret != null
				? clientSecret
				: (order.getStatus() == OrderStatus.PENDING_PAYMENT ? order.getPaymentClientSecret() : null);

		return new OrderResponseDTO(order.getId(), order.getStatus(), order.getTotalAmount(), order.getShippingFee(),
				resolvedClientSecret, order.getShippingAddress(), order.getExpiresAt(), order.getCreatedAt(),
				toItemDTOs(order));
	}

	private AdminOrderResponseDTO toAdminResponseDTO(Order order) {
		User customer = order.getUser();

		return new AdminOrderResponseDTO(order.getId(), order.getStatus(), order.getTotalAmount(),
				order.getShippingFee(), customer.getId(), customer.getFirstName() + " " + customer.getLastName(),
				customer.getEmail(), order.getShippingAddress(), order.getExpiresAt(), order.getCreatedAt(),
				toItemDTOs(order));
	}

	private List<OrderItemResponseDTO> toItemDTOs(Order order) {
		return order.getItems().stream()
				.map(item -> new OrderItemResponseDTO(item.getId(),
						item.getSku() != null ? item.getSku().getId() : null, item.getProductName(), item.getSkuCode(),
						item.getSize(), item.getColor(), item.getImageUrl(), item.getPriceAtPurchase(),
						item.getListPriceAtPurchase(), item.getQuantity()))
				.toList();
	}
}
