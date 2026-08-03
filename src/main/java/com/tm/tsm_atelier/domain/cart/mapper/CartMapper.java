package com.tm.tsm_atelier.domain.cart.mapper;

import com.tm.tsm_atelier.domain.cart.dto.CartItemResponseDTO;
import com.tm.tsm_atelier.domain.cart.dto.CartResponseDTO;
import com.tm.tsm_atelier.domain.cart.entity.Cart;
import com.tm.tsm_atelier.domain.cart.entity.CartItem;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

	public CartResponseDTO toResponse(Cart cart) {
		if (cart == null) {
			return null;
		}

		List<CartItemResponseDTO> items = cart.getItems().stream().map(this::toItemResponse)
				.collect(Collectors.toList());

		Integer totalItems = items.stream().mapToInt(CartItemResponseDTO::quantity).sum();

		BigDecimal totalPrice = items.stream().map(CartItemResponseDTO::subtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		return new CartResponseDTO(cart.getId(), items, totalItems, totalPrice);
	}

	private CartItemResponseDTO toItemResponse(CartItem item) {
		ProductSKU sku = item.getSku();
		ProductColor color = sku.getProductColor();
		Product product = color.getProduct();

		BigDecimal unitPrice = product.getPrice();
		BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

		boolean available = product.isActive() && product.getDeletedAt() == null && sku.getDeletedAt() == null
				&& sku.getStockQuantity() > 0;

		return new CartItemResponseDTO(item.getId(), sku.getId(), sku.getSkuCode(), sku.getSize().name(),
				product.getId(), product.getName(), product.getSlug(), color.getColorName(), color.getCoverImageUrl(),
				item.getQuantity(), unitPrice, subtotal, sku.getStockQuantity(), available);
	}
}
