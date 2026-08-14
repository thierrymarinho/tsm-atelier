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
@SQLDelete(sql = "UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
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

	@Version
	private Long version;

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
