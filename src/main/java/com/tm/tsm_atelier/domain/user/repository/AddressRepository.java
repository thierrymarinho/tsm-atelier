package com.tm.tsm_atelier.domain.user.repository;

import com.tm.tsm_atelier.domain.user.entity.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
	List<Address> findByUserIdOrderByCreatedAtAsc(UUID userId);
	long countByUserId(UUID userId);
	Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
