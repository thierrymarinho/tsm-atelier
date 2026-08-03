package com.tm.tsm_atelier.common.exception.custom;

public class OutOfStockException extends RuntimeException {

	private final Integer availableQuantity;

	public OutOfStockException(String message, Integer availableQuantity) {
		super(message);
		this.availableQuantity = availableQuantity;
	}

	public Integer getAvailableQuantity() {
		return availableQuantity;
	}
}
