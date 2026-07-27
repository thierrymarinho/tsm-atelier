package com.tm.tsm_atelier.common.exception.custom;

public class EmailAlreadyExistsException extends RuntimeException {
	public EmailAlreadyExistsException(String message) {
		super(message);
	}
}
