package com.tm.tsm_atelier.domain.order.service;

import com.tm.tsm_atelier.common.exception.custom.AddressNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
			AddressRepository addressRepository, ApplicationEventPublisher eventPublisher, CartService cartService) {
		this.orderRepository = orderRepository;
		this.skuRepository = skuRepository;
		this.addressRepository = addressRepository;
		this.eventPublisher = eventPublisher;
		this.cartService = cartService;
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
	 * Quem disparou a ação, para o rastro de auditoria. Lido do contexto de
	 * segurança em vez de virar parâmetro do método porque é informação de log, e
	 * não de negócio — a assinatura pública do serviço não deveria mudar por causa
	 * disso. Cai em "system" quando não há requisição autenticada por trás, como no
	 * scheduler de expiração.
	 */
	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? "system" : authentication.getName();
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
			skuRepository.findByIdWithPessimisticLock(item.getSku().getId())
					.ifPresent(sku -> sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity()));
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
		// Um único pedido pode usar fetch join com segurança — não há paginação
		// envolvida, então o Hibernate resolve tudo em uma consulta.
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));

		if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
			throw new AccessDeniedException("Access denied");
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
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));

		OrderStatus previousStatus = order.getStatus();

		if (previousStatus == newStatus) {
			return toResponseDTO(order, null);
		}

		if (!ALLOWED_TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(newStatus)) {
			throw new IllegalArgumentException(
					"Cannot move an order from " + previousStatus + " to " + newStatus + ".");
		}

		// Cancelar precisa devolver o estoque reservado. A versão anterior apenas
		// trocava o status, então todo cancelamento feito pelo admin vazava o
		// estoque daquele pedido para sempre.
		if (newStatus == OrderStatus.CANCELLED) {
			restoreStock(order);
		}

		order.setStatus(newStatus);

		// Não existe tabela de auditoria: sem esta linha, quem moveu o pedido — e
		// devolveu estoque ao cancelá-lo — não fica registrado em lugar nenhum. O
		// banco guarda só o estado final, nunca quem o produziu.
		log.info("Order {} moved from {} to {} by {}", id, previousStatus, newStatus, currentActor());

		return toResponseDTO(order, null);
	}

	private OrderResponseDTO toResponseDTO(Order order, String clientSecret) {
		String resolvedClientSecret = clientSecret != null
				? clientSecret
				: (order.getStatus() == OrderStatus.PENDING_PAYMENT ? order.getPaymentClientSecret() : null);

		List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
				.map(item -> new OrderItemResponseDTO(item.getId(),
						item.getSku() != null ? item.getSku().getId() : null, item.getProductName(), item.getSkuCode(),
						item.getSize(), item.getColor(), item.getImageUrl(), item.getPriceAtPurchase(),
						item.getQuantity()))
				.toList();

		return new OrderResponseDTO(order.getId(), order.getStatus(), order.getTotalAmount(), order.getShippingFee(),
				resolvedClientSecret, order.getShippingAddress(), order.getExpiresAt(), order.getCreatedAt(), itemDTOs);
	}
}
