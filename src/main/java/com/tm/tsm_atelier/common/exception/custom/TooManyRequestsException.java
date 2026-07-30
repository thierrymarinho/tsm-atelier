package com.tm.tsm_atelier.common.exception.custom;

public class TooManyRequestsException extends RuntimeException {
	public TooManyRequestsException(String message) {
		super(message);
	}
}
