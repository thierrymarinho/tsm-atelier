package com.tm.tsm_atelier.common.exception.custom;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException(String message) {
		super(message);
	}
}
