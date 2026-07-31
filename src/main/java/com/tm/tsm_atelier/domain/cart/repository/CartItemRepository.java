package com.tm.tsm_atelier.domain.cart.repository;

import com.tm.tsm_atelier.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	@Modifying
	@Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.id = :itemId")
	void deleteByCartIdAndItemId(@Param("cartId") Long cartId, @Param("itemId") Long itemId);

	@Modifying
	@Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
	void deleteAllByCartId(@Param("cartId") Long cartId);
}
