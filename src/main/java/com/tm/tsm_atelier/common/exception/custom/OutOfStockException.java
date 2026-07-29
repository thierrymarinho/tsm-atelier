package com.tm.tsm_atelier.common.exception.custom;

public class OutOfStockException extends RuntimeException {
	public OutOfStockException(String message) {
		super(message);
	}
}
