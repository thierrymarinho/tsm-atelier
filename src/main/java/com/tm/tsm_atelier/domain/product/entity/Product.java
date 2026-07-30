package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.common.utils.SlugUtils;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Product extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(unique = true)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ElementCollection
	@CollectionTable(name = "product_fabric_compositions", joinColumns = @JoinColumn(name = "product_id"))
	@Builder.Default
	private List<FabricComposition> fabricCompositions = new ArrayList<>();

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@ElementCollection
	@CollectionTable(name = "product_care_instructions", joinColumns = @JoinColumn(name = "product_id"))
	@Column(name = "instruction")
	@Builder.Default
	private Set<String> careInstructions = new java.util.LinkedHashSet<>();

	@Column(nullable = false)
	private BigDecimal price;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "collection_id")
	private Collection collection;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_audience", nullable = false)
	private TargetAudience targetAudience;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "is_featured", nullable = false)
	private boolean featured;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	@Builder.Default
	private Set<ProductColor> colors = new java.util.LinkedHashSet<>();

	@PrePersist
	public void generateSlugBeforePersist() {
		if (this.slug == null && this.name != null) {
			this.slug = SlugUtils.generateSlug(this.name);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o))
			return false;
		Product product = (Product) o;
		return id != null && id.equals(product.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
