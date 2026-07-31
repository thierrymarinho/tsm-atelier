package com.tm.tsm_atelier.domain.cart.service;

import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.cart.dto.CartItemRequestDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartResponseDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartSyncRequestDTO;
import com.tm.tsm_atelier.domain.cart.entity.Cart;
import com.tm.tsm_atelier.domain.cart.entity.CartItem;
import com.tm.tsm_atelier.domain.cart.mapper.CartMapper;
import com.tm.tsm_atelier.domain.cart.repository.CartItemRepository;
import com.tm.tsm_atelier.domain.cart.repository.CartRepository;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductSKURepository skuRepository;
	private final UserRepository userRepository;
	private final CartMapper cartMapper;

	@Transactional(readOnly = true)
	public CartResponseDTO getCart(UUID userId) {
		Cart cart = getOrCreateCartEntity(userId);
		return cartMapper.toResponse(cart);
	}

	@Transactional
	public CartResponseDTO addItem(UUID userId, CartItemRequestDTO request) {
		Cart cart = getOrCreateCartEntity(userId);

		ProductSKU sku = skuRepository.findById(request.skuId())
				.orElseThrow(() -> new ResourceNotFoundException("Product SKU", request.skuId()));

		Optional<CartItem> existingItemOpt = cart.getItems().stream()
				.filter(item -> item.getSku().getId().equals(sku.getId())).findFirst();

		int newQuantity = request.quantity();
		if (existingItemOpt.isPresent()) {
			newQuantity += existingItemOpt.get().getQuantity();
		}

		if (sku.getStockQuantity() < newQuantity) {
			throw new OutOfStockException("Not enough stock for SKU: " + sku.getSkuCode());
		}

		if (existingItemOpt.isPresent()) {
			existingItemOpt.get().setQuantity(newQuantity);
		} else {
			CartItem newItem = new CartItem();
			newItem.setCart(cart);
			newItem.setSku(sku);
			newItem.setQuantity(newQuantity);
			cart.addItem(newItem);
		}

		cart = cartRepository.save(cart);
		return cartMapper.toResponse(cart);
	}

	@Transactional
	public CartResponseDTO updateItemQuantity(UUID userId, Long itemId, Integer quantity) {
		Cart cart = getOrCreateCartEntity(userId);

		CartItem item = cart.getItems().stream().filter(ci -> ci.getId().equals(itemId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

		if (item.getSku().getStockQuantity() < quantity) {
			throw new OutOfStockException("Not enough stock for SKU: " + item.getSku().getSkuCode());
		}

		item.setQuantity(quantity);
		cart = cartRepository.save(cart);
		return cartMapper.toResponse(cart);
	}

	@Transactional
	public CartResponseDTO removeItem(UUID userId, Long itemId) {
		Cart cart = getOrCreateCartEntity(userId);

		CartItem itemToRemove = cart.getItems().stream().filter(ci -> ci.getId().equals(itemId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

		cart.removeItem(itemToRemove);
		cartItemRepository.delete(itemToRemove);

		cart = cartRepository.save(cart);
		return cartMapper.toResponse(cart);
	}

	@Transactional
	public CartResponseDTO syncCart(UUID userId, CartSyncRequestDTO request) {
		Cart cart = getOrCreateCartEntity(userId);

		for (CartItemRequestDTO reqItem : request.items()) {
			try {
				ProductSKU sku = skuRepository.findById(reqItem.skuId())
						.orElseThrow(() -> new ResourceNotFoundException("Product SKU", reqItem.skuId()));

				Optional<CartItem> existingItemOpt = cart.getItems().stream()
						.filter(item -> item.getSku().getId().equals(sku.getId())).findFirst();

				int quantityToAdd = reqItem.quantity();

				if (existingItemOpt.isPresent()) {
					// Do not accumulate if we consider frontend sent exact quantity,
					// or accumulate? The plan says "mesclar". Let's accumulate.
					int newQuantity = existingItemOpt.get().getQuantity() + quantityToAdd;
					if (sku.getStockQuantity() >= newQuantity) {
						existingItemOpt.get().setQuantity(newQuantity);
					} else {
						// Max out to stock if syncing
						existingItemOpt.get().setQuantity(sku.getStockQuantity());
					}
				} else {
					if (sku.getStockQuantity() >= quantityToAdd) {
						CartItem newItem = new CartItem();
						newItem.setCart(cart);
						newItem.setSku(sku);
						newItem.setQuantity(quantityToAdd);
						cart.addItem(newItem);
					} else if (sku.getStockQuantity() > 0) {
						CartItem newItem = new CartItem();
						newItem.setCart(cart);
						newItem.setSku(sku);
						newItem.setQuantity(sku.getStockQuantity());
						cart.addItem(newItem);
					}
				}
			} catch (Exception e) {
				// Ignore items that cause errors during sync (e.g., deleted SKUs)
			}
		}

		cart = cartRepository.save(cart);
		return cartMapper.toResponse(cart);
	}

	@Transactional
	public void clearCart(UUID userId) {
		Cart cart = getOrCreateCartEntity(userId);
		cartItemRepository.deleteAllByCartId(cart.getId());
		cart.clearItems();
		cartRepository.save(cart);
	}

	private Cart getOrCreateCartEntity(UUID userId) {
		return cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new ResourceNotFoundException("User", userId));
			Cart newCart = new Cart();
			newCart.setUser(user);
			return cartRepository.save(newCart);
		});
	}
}
