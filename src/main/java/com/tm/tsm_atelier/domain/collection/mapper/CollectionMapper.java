package com.tm.tsm_atelier.domain.collection.mapper;

import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Os ignore são declarados um a um, e não silenciados por unmappedTargetPolicy.
 * A diferença importa: uma política global também engoliria um campo novo do
 * DTO que deveria ser mapeado e não é, que é o único aviso que o MapStruct dá
 * desse erro.
 *
 * O slug é o caso que justifica o cuidado. Ele é gerado na criação por
 * CollectionService e congelado daí em diante — o link publicado não muda
 * quando a coleção é renomeada. Até aqui essa garantia dependia de o DTO não
 * ter um campo de mesmo nome: bastaria alguém acrescentar slug a
 * CollectionRequestDTO para o MapStruct passar a mapeá-lo, em silêncio, e o
 * contrato quebrar sem nenhum erro de compilação. Declarado, ele continua fora
 * do mapeamento aconteça o que acontecer.
 */
@Mapper(componentModel = "spring")
public interface CollectionMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "products", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	Collection toEntity(CollectionRequestDTO request);

	CollectionResponseDTO toResponse(Collection entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "products", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void updateEntityFromRequest(CollectionRequestDTO request, @MappingTarget Collection entity);
}
