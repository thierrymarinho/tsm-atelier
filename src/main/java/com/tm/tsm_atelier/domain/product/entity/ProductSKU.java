package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_skus")
@SQLDelete(sql = "UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
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

	@Column(nullable = false, length = 20)
	@Enumerated(jakarta.persistence.EnumType.STRING)
	private com.tm.tsm_atelier.domain.product.enums.ProductSize size;

	@Column(name = "sku_code", nullable = false, unique = true)
	private String skuCode;

	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
