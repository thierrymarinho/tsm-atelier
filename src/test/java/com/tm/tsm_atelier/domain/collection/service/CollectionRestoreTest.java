package com.tm.tsm_atelier.domain.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * A restauração de coleção precisa de banco: o {@code @SQLRestriction} da
 * entidade some com a linha em toda consulta JPQL, então o que está sendo
 * testado é justamente o caminho nativo que enxerga em volta dele.
 */
@SpringBootTest
@Transactional
@DisplayName("Collection restore")
class CollectionRestoreTest {

	@Autowired
	private CollectionService collectionService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("A deleted collection can be restored, and comes back inactive")
	void deletedCollectionCanBeRestored() {
		CollectionResponseDTO created = collectionService.create(aCollection("Verão Restaurável"));
		reload();

		collectionService.delete(created.id(), false);
		reload();

		// O beco sem saída antes desta rota: a coleção some de tudo, inclusive do GET
		// por id — o admin não tinha nem como saber que ela ainda existia.
		assertThatThrownBy(() -> collectionService.findById(created.id()))
				.isInstanceOf(ResourceNotFoundException.class);

		CollectionResponseDTO restored = collectionService.restore(created.id());

		assertThat(restored.name()).isEqualTo("Verão Restaurável");
		assertThat(restored.active()).as("recuperar o cadastro e republicar são duas decisões").isFalse();
		assertThatCode(() -> collectionService.findById(created.id())).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Restoring a collection that is not deleted is refused")
	void restoringALiveCollectionIsRefused() {
		CollectionResponseDTO created = collectionService.create(aCollection("Coleção Viva"));
		reload();

		assertThatThrownBy(() -> collectionService.restore(created.id())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("is not deleted");
	}

	@Test
	@DisplayName("Restoring an id that never existed is a 404")
	void restoringAnUnknownIdIsNotFound() {
		assertThatThrownBy(() -> collectionService.restore(999_999L)).isInstanceOf(ResourceNotFoundException.class);
	}

	/**
	 * {@code uk_collection_name_audience} é uma constraint <strong>total</strong>,
	 * então a coleção removida continua ocupando o nome. A validação, sendo JPQL,
	 * não a enxergava: o insert passava por ela, quebrava no banco e voltava como
	 * 409 "A data conflict occurred" apontando um registro invisível.
	 */
	@Test
	@DisplayName("Should explain that a deleted collection is holding the name, and how to get it back")
	void nameHeldByADeletedCollectionIsExplained() {
		CollectionResponseDTO created = collectionService.create(aCollection("Nome Disputado"));
		reload();

		collectionService.delete(created.id(), false);
		reload();

		assertThatThrownBy(() -> collectionService.create(aCollection("Nome Disputado")))
				.isInstanceOf(EntityAlreadyExistsException.class).hasMessageContaining(String.valueOf(created.id()))
				.hasMessageContaining("/restore");
	}

	/**
	 * O slug tem constraint total pelo mesmo motivo, e o gerador contava só as
	 * vivas — devolvia um slug já ocupado, e o insert morria no banco.
	 */
	@Test
	@DisplayName("Should not hand out a slug that a deleted collection still holds")
	void slugHeldByADeletedCollectionIsSkipped() {
		CollectionResponseDTO created = collectionService.create(aCollection("Slug Disputado"));
		reload();

		collectionService.delete(created.id(), false);
		reload();

		// Mesmo nome-base, público diferente: passa pela unicidade de
		// (name, target_audience) e chega no gerador de slug.
		CollectionResponseDTO other = collectionService.create(new CollectionRequestDTO("Slug Disputado", true, "d",
				null, null, null, DisplayPosition.NONE, 0, TargetAudience.MEN));

		assertThat(other.slug()).isNotEqualTo(created.slug());
	}

	/**
	 * O mesmo formato dos dois casos acima, na terceira constraint — e esta ficou
	 * aberta mais tempo porque o índice é parcial, o que dá a impressão de já estar
	 * resolvida.
	 *
	 * <p>
	 * Enquanto {@code uk_one_home_main} filtrou só por {@code display_position},
	 * sem olhar {@code deleted_at}, a coleção no lixo seguia ocupando o HOME_MAIN
	 * do site inteiro — a exclusão lógica não limpa a posição. O
	 * {@code invalidateExistingDisplayPositions} não podia rebaixá-la — é JPQL, não
	 * a enxerga —, então o insert passava pelas checagens e morria no flush. É o
	 * {@code AND deleted_at IS NULL} do V2 que este teste protege.
	 */
	@Test
	@DisplayName("Should let a new collection take a display position that only a deleted one still holds")
	void displayPositionHeldByADeletedCollectionIsReleased() {
		CollectionResponseDTO created = collectionService.create(atHomeMain("Destaque Antigo"));
		reload();

		collectionService.delete(created.id(), false);
		reload();

		assertThatCode(() -> {
			collectionService.create(atHomeMain("Destaque Novo"));
			reload();
		}).doesNotThrowAnyException();
	}

	/**
	 * A contrapartida do teste acima. Liberar a posição na exclusão significa que
	 * ela pode ter dono quando a coleção voltar — e aí a restauração é que
	 * quebraria no índice, longe do código que a causou.
	 */
	@Test
	@DisplayName("A restored collection comes back without its display position")
	void restoredCollectionGivesUpItsDisplayPosition() {
		CollectionResponseDTO created = collectionService.create(atHomeMain("Destaque Emprestado"));
		reload();

		collectionService.delete(created.id(), false);
		reload();

		collectionService.create(atHomeMain("Destaque Tomado"));
		reload();

		CollectionResponseDTO restored = collectionService.restore(created.id());
		reload();

		assertThat(restored.displayPosition())
				.as("a posição foi ocupada no intervalo; voltar com ela quebraria o índice")
				.isEqualTo(DisplayPosition.NONE);
	}

	private CollectionRequestDTO atHomeMain(String name) {
		return new CollectionRequestDTO(name, true, "Descrição", null, null, null, DisplayPosition.HOME_MAIN, 0,
				TargetAudience.WOMEN);
	}

	private CollectionRequestDTO aCollection(String name) {
		return new CollectionRequestDTO(name, true, "Descrição", null, null, null, DisplayPosition.NONE, 0,
				TargetAudience.WOMEN);
	}

	private void reload() {
		entityManager.flush();
		entityManager.clear();
	}
}
