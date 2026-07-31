package com.tm.tsm_atelier.domain.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDTO(Long id, List<CartItemResponseDTO> items, Integer totalItems, BigDecimal totalPrice) {
}
