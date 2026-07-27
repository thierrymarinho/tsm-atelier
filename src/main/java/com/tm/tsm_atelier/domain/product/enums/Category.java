package com.tm.tsm_atelier.domain.product.enums;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public enum Category {
	JACKETS, COATS_AND_TRENCHES, DRESSES, BLAZERS, SHIRTS_AND_BLOUSES, JEANS, T_SHIRTS, SHIRTS, SKIRTS_AND_SHORTS, SHORTS;

	private static final Map<TargetAudience, EnumSet<Category>> ALLOWED_BY_AUDIENCE;

	static {
		ALLOWED_BY_AUDIENCE = new EnumMap<>(TargetAudience.class);

		EnumSet<Category> womenCategories = EnumSet.of(DRESSES, JACKETS, COATS_AND_TRENCHES, SHIRTS_AND_BLOUSES, JEANS,
				T_SHIRTS, SKIRTS_AND_SHORTS);

		ALLOWED_BY_AUDIENCE.put(TargetAudience.WOMEN, womenCategories);

		EnumSet<Category> menCategories = EnumSet.of(JACKETS, COATS_AND_TRENCHES, BLAZERS, T_SHIRTS, SHIRTS, JEANS,
				SHORTS);

		ALLOWED_BY_AUDIENCE.put(TargetAudience.MEN, menCategories);
	}

	public boolean isValidFor(TargetAudience audience) {
		if (audience == null) {
			return true;
		}
		return ALLOWED_BY_AUDIENCE.getOrDefault(audience, EnumSet.noneOf(Category.class)).contains(this);
	}

	public static List<Category> getByTargetAudience(TargetAudience audience) {
		if (audience == null) {
			return List.of(values());
		}
		return Arrays.stream(values()).filter(category -> category.isValidFor(audience)).toList();
	}
}
