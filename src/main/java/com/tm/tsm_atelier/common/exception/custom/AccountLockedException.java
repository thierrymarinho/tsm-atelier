package com.tm.tsm_atelier.common.exception.custom;

public class AccountLockedException extends RuntimeException {
	public AccountLockedException(String message) {
		super(message);
	}
}
