package com.tm.tsm_atelier.domain.cart.repository;

import com.tm.tsm_atelier.domain.cart.entity.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

	@Query("SELECT c FROM Cart c " + "LEFT JOIN FETCH c.items i " + "LEFT JOIN FETCH i.sku s "
			+ "LEFT JOIN FETCH s.productColor pc " + "LEFT JOIN FETCH pc.product p " + "WHERE c.user.id = :userId")
	Optional<Cart> findByUserIdWithItems(@Param("userId") UUID userId);

	Optional<Cart> findByUserId(UUID userId);
}
