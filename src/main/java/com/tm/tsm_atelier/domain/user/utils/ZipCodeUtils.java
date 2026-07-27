package com.tm.tsm_atelier.domain.user.utils;

public final class ZipCodeUtils {

	private ZipCodeUtils() {
	}

	/**
	 * Formata o CEP deixando apenas os números.
	 *
	 * @param zipCode
	 *            CEP que pode vir no formato 00000-000 ou 00000000
	 * @return CEP contendo apenas números ou nulo se a entrada for nula
	 */
	public static String formatZipCode(String zipCode) {
		if (zipCode == null) {
			return null;
		}
		return zipCode.replaceAll("\\D", "");
	}
}
