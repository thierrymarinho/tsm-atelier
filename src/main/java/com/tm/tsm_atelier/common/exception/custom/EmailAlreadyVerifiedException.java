package com.tm.tsm_atelier.common.exception.custom;

public class EmailAlreadyVerifiedException extends RuntimeException {
	public EmailAlreadyVerifiedException(String message) {
		super(message);
	}
}
