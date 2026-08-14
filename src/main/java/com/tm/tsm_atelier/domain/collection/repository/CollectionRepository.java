package com.tm.tsm_atelier.domain.collection.repository;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {

	Optional<Collection> findByNameAndTargetAudience(String name, TargetAudience targetAudience);

	/**
	 * A rota pública por slug. O {@code @SQLRestriction} já filtra as removidas.
	 */
	Optional<Collection> findBySlug(String slug);

	/**
	 * O par de consultas de posição de destaque é assimétrico de propósito, e
	 * acompanha os índices do V2: {@code uk_one_home_main} é global — existe uma
	 * HOME_MAIN no site inteiro —, enquanto HOME_SECONDARY e HEADER são únicos por
	 * público.
	 */
	Optional<Collection> findByDisplayPositionAndTargetAudience(DisplayPosition displayPosition,
			TargetAudience targetAudience);

	Optional<Collection> findByDisplayPosition(DisplayPosition displayPosition);

	/**
	 * Nativa por necessidade: o {@code @SQLRestriction} da entidade esconde
	 * coleções removidas de toda consulta JPQL, inclusive do {@code findById} — e a
	 * restauração precisa justamente delas.
	 */
	@Query(value = "SELECT * FROM collections WHERE id = :id", nativeQuery = true)
	Optional<Collection> findByIdIncludingDeleted(@Param("id") Long id);

	/**
	 * A posição sai na mesma instrução que o {@code deleted_at}, e não depois, em
	 * Java. Os índices de destaque ignoram as removidas, então limpar só o
	 * {@code deleted_at} devolve a linha ao índice ainda carregando a posição
	 * antiga — e se alguém a ocupou no intervalo, este próprio UPDATE viola
	 * {@code uk_one_home_main}. Não existe janela entre as duas colunas para o
	 * serviço aproveitar.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "UPDATE collections SET deleted_at = NULL, display_position = 'NONE' WHERE id = :id", nativeQuery = true)
	int restoreCollection(@Param("id") Long id);

	/**
	 * As constraints {@code uk_collection_name_audience} e
	 * {@code uk_collection_slug} são <strong>totais</strong>, e não parciais como o
	 * índice de {@code sku_code}: uma coleção removida continua ocupando o nome e o
	 * slug. As duas consultas abaixo enxergam essas linhas, que é o que permite
	 * dizer isso ao admin em vez de deixá-lo colidir com o banco.
	 */
	@Query(value = """
			SELECT id FROM collections
			WHERE name = :name AND target_audience = :targetAudience AND deleted_at IS NOT NULL
			""", nativeQuery = true)
	Optional<Long> findDeletedIdByNameAndTargetAudience(@Param("name") String name,
			@Param("targetAudience") String targetAudience);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM collections WHERE slug = :slug)", nativeQuery = true)
	boolean existsBySlugIncludingDeleted(@Param("slug") String slug);
}
