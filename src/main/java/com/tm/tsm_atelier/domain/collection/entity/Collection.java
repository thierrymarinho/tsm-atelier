package com.tm.tsm_atelier.domain.collection.entity;

import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "collections", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "target_audience"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "image_url")
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "display_position")
	@Builder.Default
	private DisplayPosition displayPosition = DisplayPosition.NONE;

	@Column(name = "display_order")
	@Builder.Default
	private Integer displayOrder = 0;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_audience", nullable = false)
	@Builder.Default
	private TargetAudience targetAudience = TargetAudience.WOMEN;

	@OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Product> products = new ArrayList<>();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
		Collection that = (Collection) o;
		return id != null && id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
