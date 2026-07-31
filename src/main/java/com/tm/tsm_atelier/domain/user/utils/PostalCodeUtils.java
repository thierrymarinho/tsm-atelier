package com.tm.tsm_atelier.domain.user.utils;

public final class PostalCodeUtils {

	private PostalCodeUtils() {
	}

	public static String formatZipCode(String zipCode) {
		if (zipCode == null) {
			return null;
		}
		return zipCode.replaceAll("\\D", "");
	}
}
