package com.tm.tsm_atelier.domain.user.utils;

public final class PostalCodeUtils {

	private PostalCodeUtils() {
	}

	public static String formatPostalCode(String postalCode) {
		if (postalCode == null) {
			return null;
		}
		return postalCode.replaceAll("\\D", "");
	}
}
