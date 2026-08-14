package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Material;

public record MaterialOptionDTO(String name, String label) {

	public static MaterialOptionDTO from(Material material) {
		return new MaterialOptionDTO(material.name(), material.getLabel());
	}
}
