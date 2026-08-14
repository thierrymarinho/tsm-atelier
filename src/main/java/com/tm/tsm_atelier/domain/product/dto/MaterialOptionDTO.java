package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Material;

/**
 * O vocabulário de composição servido para quem monta o formulário. Sem este
 * endpoint o front precisaria repetir a lista em codigo — e um vocabulario
 * mantido em dois lugares e a mesma divergencia que o enum acabou de fechar, so
 * que uma camada acima.
 */
public record MaterialOptionDTO(String name, String label) {

	public static MaterialOptionDTO from(Material material) {
		return new MaterialOptionDTO(material.name(), material.getLabel());
	}
}
