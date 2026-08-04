package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_skus")
@SQLDelete(sql = "UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_color_id", nullable = false)
	private ProductColor productColor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private ProductSize size;

	@Column(name = "sku_code", nullable = false, unique = true, length = 100)
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
