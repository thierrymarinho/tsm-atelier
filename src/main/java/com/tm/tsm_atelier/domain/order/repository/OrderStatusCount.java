package com.tm.tsm_atelier.domain.order.repository;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;

public record OrderStatusCount(OrderStatus status, long total) {
}
