package com.tm.tsm_atelier.common.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

	private SlugUtils() {
	}

	public static String generateSlug(String name) {
		if (name == null) {
			return "produto";
		}

		String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");

		return base.isEmpty() ? "produto" : base;
	}
}
