package com.tm.tsm_atelier.domain.collection.mapper;

import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CollectionMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "products", ignore = true)
	Collection toEntity(CollectionRequestDTO request);

	CollectionResponseDTO toResponse(Collection entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "products", ignore = true)
	void updateEntityFromRequest(CollectionRequestDTO request, @MappingTarget Collection entity);
}
