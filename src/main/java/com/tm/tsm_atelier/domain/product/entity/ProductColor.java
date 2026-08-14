package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_colors")
@SQLDelete(sql = "UPDATE product_colors SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
// Mesma razão do @SQLRestriction em ProductSKU, e a ausência dele aqui quebrava
// o formulário de edição do admin: remover uma cor no PUT marcava deleted_at,
// mas
// a leitura seguinte trazia a cor de volta — e sem os SKUs, que o
// @SQLRestriction
// deles já escondia. O @NotEmpty de ProductColorRequestDTO.skus então recusava
// todo salvamento posterior com 422, numa cor que o admin achava ter apagado.
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColor extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "color_name", nullable = false, length = 100)
	private String colorName;

	@Column(name = "color_hex", nullable = false, length = 10)
	private String colorHex;

	@Column(name = "cover_image_url", length = 500)
	private String coverImageUrl;

	@Column(name = "hover_image_url", length = 500)
	private String hoverImageUrl;

	@ElementCollection
	@CollectionTable(name = "product_gallery_images", joinColumns = @JoinColumn(name = "product_color_id"))
	@Column(name = "image_url", length = 500)
	@Builder.Default
	private Set<String> galleryImages = new LinkedHashSet<>();

	@OneToMany(mappedBy = "productColor", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	@Builder.Default
	private Set<ProductSKU> skus = new LinkedHashSet<>();

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o))
			return false;
		ProductColor that = (ProductColor) o;
		return id != null && id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
