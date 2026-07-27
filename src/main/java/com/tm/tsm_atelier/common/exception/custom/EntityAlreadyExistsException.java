package com.tm.tsm_atelier.common.exception.custom;

import lombok.Getter;

@Getter
public class EntityAlreadyExistsException extends RuntimeException {
	private final String entityName;
	private final Object identifier;

	public EntityAlreadyExistsException(String entityName, Object identifier) {
		super(entityName + " already exists with identifier: " + identifier);
		this.entityName = entityName;
		this.identifier = identifier;
	}
}
