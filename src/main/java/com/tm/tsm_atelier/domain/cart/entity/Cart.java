package com.tm.tsm_atelier.domain.cart.entity;

import com.tm.tsm_atelier.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class Cart {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	public void addItem(CartItem item) {
		items.add(item);
		item.setCart(this);
	}

	public void removeItem(CartItem item) {
		items.remove(item);
		item.setCart(null);
	}

	public void clearItems() {
		items.forEach(item -> item.setCart(null));
		items.clear();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Cart cart))
			return false;
		return id != null && id.equals(cart.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
