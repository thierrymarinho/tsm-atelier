package com.tm.tsm_atelier.domain.order.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record OrderSearchFilter(OrderStatus status, String searchTerm,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
}
