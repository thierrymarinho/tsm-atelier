package com.tm.tsm_atelier.common.exception.custom;

public class AddressLimitExceededException extends RuntimeException {
	public AddressLimitExceededException(String message) {
		super(message);
	}
}
