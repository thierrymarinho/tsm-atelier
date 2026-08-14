package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_skus")
// O "AND version = ?" não é opcional depois do @Version: numa entidade
// versionada
// o Hibernate liga id e versão no delete, e o SQL com um único placeholder
// quebrava com "column index is out of range" em toda remoção de SKU.
@SQLDelete(sql = "UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
// Sem o @SQLRestriction o soft-delete apenas escondia o SKU do catálogo: um
// findById direto seguia devolvendo o registro, permitindo comprar um produto
// que a loja já havia retirado de venda.
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSKU extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Protege gravações concorrentes na mesma linha entre carregar e gravar dentro
	 * de uma transação — o que o Hibernate faz sozinho, sem ninguém precisar
	 * comparar nada.
	 *
	 * <p>
	 * A versão também sai daqui para o cliente, no ProductSKUResponseDTO, mas serve
	 * a um único caso: a contagem de inventário do {@code PATCH
	 * /api/v1/admin/skus/{id}/stock}, onde o admin envia um valor absoluto e
	 * precisa saber se o estoque se moveu entre a contagem e o salvamento. Ajuste
	 * por delta não usa versão nenhuma — soma é comutativa, duas operações
	 * simultâneas compõem em vez de se sobrescrever.
	 */
	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_color_id", nullable = false)
	private ProductColor productColor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private ProductSize size;

	// Sem unique = true: o índice do banco (idx_sku_code_active) é parcial, só
	// cobre linhas com deleted_at IS NULL. Declarar unicidade total aqui descrevia
	// uma constraint que não existe.
	@Column(name = "sku_code", nullable = false, length = 100)
	private String skuCode;

	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o))
			return false;
		ProductSKU that = (ProductSKU) o;
		return id != null && id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
