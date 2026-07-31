package com.tm.tsm_atelier.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

	@Column(nullable = false)
	private String street;

	@Column(nullable = false, length = 10)
	private String number;

	@Column(length = 255)
	private String complement;

	@Column(nullable = false, length = 100)
	private String neighborhood;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(nullable = false, length = 2)
	private String state;

	@Column(name = "postal_code", nullable = false, length = 8)
	private String postalCode;
}
