package com.tm.tsm_atelier.domain.order.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sku_id", nullable = true)
	private ProductSKU sku;

	private String productName;

	private String skuCode;

	private String size;

	private String color;

	/**
	 * Copiado de ProductColor.coverImageUrl, que tem length 500. O default do
	 * Hibernate seria 255, menor que a origem — e o insert do pedido falharia com
	 * uma URL longa.
	 */
	@Column(length = 500)
	private String imageUrl;

	/** O que foi efetivamente cobrado — promocional, quando havia promocao. */
	private BigDecimal priceAtPurchase;

	/**
	 * O preco de tabela no momento da compra. Sem ele o desconto sumiria do
	 * historico: o pedido saberia quanto custou, mas nao que houve promocao.
	 */
	@Column(name = "list_price_at_purchase", nullable = false)
	private BigDecimal listPriceAtPurchase;

	private Integer quantity;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o))
			return false;
		OrderItem orderItem = (OrderItem) o;
		return id != null && id.equals(orderItem.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
