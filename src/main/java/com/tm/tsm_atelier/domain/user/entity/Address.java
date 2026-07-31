package com.tm.tsm_atelier.domain.user.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.user.enums.State;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String street;

	@Column(nullable = false, length = 10)
	private String number;

	@Column(length = 255)
	private String complement;

	@Column(nullable = false, length = 100)
	private String neighborhood;

	@Column(nullable = false, length = 100)
	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 2)
	private State state;

	@Column(name = "postal_code", nullable = false, length = 8)
	private String postalCode;

	@Column(nullable = false)
	@Builder.Default
	private boolean isDefault = false;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o))
			return false;
		Address address = (Address) o;
		return id != null && id.equals(address.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
