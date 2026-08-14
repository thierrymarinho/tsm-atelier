package com.tm.tsm_atelier.domain.product.repository;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSKURepository extends JpaRepository<ProductSKU, Long> {
	Optional<ProductSKU> findBySkuCode(String skuCode);

	/**
	 * O próximo número de {@code sku_code}. A sequência é criada no V3, e o formato
	 * montado em {@code ProductService.generateSkuCode}.
	 *
	 * <p>
	 * Aqui morava um trio de checagens — {@code findExistingSkuCodes},
	 * {@code existsBySkuCode} e {@code existsBySkuCodeAndIdNot} — que existia
	 * porque o admin digitava o código. Com ele saindo daqui, "esse código já está
	 * em uso" deixou de ser uma pergunta que a aplicação precisa fazer: a sequência
	 * nunca repete um número.
	 */
	@Query(value = "SELECT nextval('sku_code_seq')", nativeQuery = true)
	Long nextSkuCodeNumber();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ProductSKU s WHERE s.id = :id")
	Optional<ProductSKU> findByIdWithPessimisticLock(@Param("id") Long id);

	/**
	 * Nativa por necessidade: a restauração precisa alcançar linhas que o
	 * {@code @SQLRestriction} esconde de toda consulta de entidade.
	 *
	 * <p>
	 * O filtro por {@code deleted_at = :deletedAt} é o que separa "removido junto
	 * com o produto" de "removido antes, de propósito". ProductService.delete usa
	 * um único timestamp para produto, cores e SKUs, então restaurar por igualdade
	 * de instante devolve só o que a exclusão do produto levou — uma cor que o
	 * admin tinha apagado semanas antes continua apagada.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE product_skus SET deleted_at = NULL
			WHERE deleted_at = :deletedAt
			  AND product_color_id IN (SELECT id FROM product_colors WHERE product_id = :productId)
			""", nativeQuery = true)
	int restoreSkusOfProduct(@Param("productId") Long productId, @Param("deletedAt") java.time.LocalDateTime deletedAt);

	/**
	 * Códigos que impediriam a restauração porque um SKU vivo já os ocupa. Sem esta
	 * checagem o UPDATE quebraria no índice parcial e voltaria como 409 genérico,
	 * sem dizer qual código está em conflito.
	 *
	 * <p>
	 * Desde que o código passou a sair da sequência, isto é rede e não caminho:
	 * dois SKUs só carregam o mesmo código se alguém tiver escrito na tabela por
	 * fora — o seed do V9 faz exatamente isso.
	 */
	@Query(value = """
			SELECT s.sku_code FROM product_skus s
			JOIN product_colors c ON c.id = s.product_color_id
			WHERE c.product_id = :productId
			  AND s.deleted_at = :deletedAt
			  AND EXISTS (SELECT 1 FROM product_skus live
			              WHERE live.sku_code = s.sku_code AND live.deleted_at IS NULL)
			""", nativeQuery = true)
	List<String> findSkuCodesBlockingRestore(@Param("productId") Long productId,
			@Param("deletedAt") java.time.LocalDateTime deletedAt);

	/**
	 * O alerta de estoque baixo do dashboard. Só produtos <strong>ativos e não
	 * removidos</strong> entram: o alerta existe para avisar que a loja está
	 * prestes a perder venda, e um produto fora da vitrine não perde venda nenhuma.
	 * Deixá-lo na lista transformaria o painel num inventário de rascunhos.
	 *
	 * <p>
	 * SKU e cor removidos já ficam de fora pelo {@code @SQLRestriction} das
	 * entidades; {@code Product} não tem a anotação, por isso o filtro explícito.
	 */
	@Query("""
			SELECT s FROM ProductSKU s
			JOIN s.productColor c JOIN c.product p
			WHERE p.deletedAt IS NULL AND p.active = true AND s.stockQuantity <= :threshold
			ORDER BY s.stockQuantity ASC, s.id ASC
			""")
	List<ProductSKU> findLowStock(@Param("threshold") int threshold, org.springframework.data.domain.Pageable pageable);

	@Query("""
			SELECT COUNT(s) FROM ProductSKU s
			JOIN s.productColor c JOIN c.product p
			WHERE p.deletedAt IS NULL AND p.active = true AND s.stockQuantity <= :threshold
			""")
	long countLowStock(@Param("threshold") int threshold);
}
