package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.product.enums.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricComposition {

	/**
	 * {@code STRING} e não {@code ORDINAL}: a coluna é metade da chave primária da
	 * tabela, e com ordinal reordenar as constantes do {@link Material}
	 * reescreveria a composição de todo o catálogo em silêncio.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Material material;

	@Column(nullable = false)
	private Integer percentage;
}
