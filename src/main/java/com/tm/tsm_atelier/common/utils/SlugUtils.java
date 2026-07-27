package com.tm.tsm_atelier.common.utils;

import org.springframework.stereotype.Component;

@Component
public class SlugUtils {

	public static String generateSlug(String name) {
		return name.toLowerCase().replaceAll("[áàãâä]", "a").replaceAll("[éèêë]", "e").replaceAll("[íìîï]", "i")
				.replaceAll("[óòõôö]", "o").replaceAll("[úùûü]", "u").replaceAll("[ç]", "c")
				.replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-").trim();
	}
}
