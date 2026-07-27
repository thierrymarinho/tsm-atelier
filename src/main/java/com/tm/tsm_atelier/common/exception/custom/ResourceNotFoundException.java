package com.tm.tsm_atelier.common.exception.custom;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

	private final String resourceName;
	private final Object identifier;

	public ResourceNotFoundException(String resourceName, Object identifier) {
		super(resourceName + " not found with identifier: " + identifier);
		this.resourceName = resourceName;
		this.identifier = identifier;
	}
}
