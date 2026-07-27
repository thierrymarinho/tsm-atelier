package com.tm.tsm_atelier.domain.user.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(nullable = false, length = 50)
	private String number;

	@Column
	private String complement;

	@Column(nullable = false, length = 100)
	private String neighborhood;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(nullable = false, length = 50)
	private String state;

	@Column(nullable = false, length = 20)
	private String zipCode;

	@Column(nullable = false)
	@Builder.Default
	private boolean isDefault = false;
}
