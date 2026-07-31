package com.tm.tsm_atelier.domain.cart.entity;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
public class CartItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sku_id", nullable = false)
	private ProductSKU sku;

	@Column(nullable = false)
	private Integer quantity;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof CartItem cartItem))
			return false;
		return id != null && id.equals(cartItem.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
