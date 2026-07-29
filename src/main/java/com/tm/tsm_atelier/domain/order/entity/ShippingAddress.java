package com.tm.tsm_atelier.domain.order.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

	private String street;
	private String number;
	private String complement;
	private String neighborhood;
	private String city;
	private String state;
	private String zipCode;
}
