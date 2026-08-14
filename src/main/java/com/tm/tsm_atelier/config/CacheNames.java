package com.tm.tsm_atelier.config;

public final class CacheNames {

	public static final String CATALOG_PRODUCTS = "catalog_products";

	public static final String CATALOG_SLUG = "catalog_slug";

	/**
	 * O detalhe da coleção por slug, separado de {@link #CATALOG_SLUG} porque um
	 * cache do Redis tem <strong>um</strong> serializer de valor por nome.
	 *
	 * <p>
	 * Enquanto produto e coleção dividiram {@code catalog_slug}, o serializer
	 * estava fixado em {@code ProductResponseDTO}: a coleção era gravada sem
	 * reclamar e toda leitura dela quebrava ao forçar o JSON para dentro do tipo
	 * errado. O {@code CacheErrorHandler} engolia a falha e caía no banco, então a
	 * resposta saía correta — o cache só nunca acertava, e logava um WARN por
	 * requisição numa rota pública.
	 */
	public static final String CATALOG_SLUG_COLLECTION = "catalog_slug_collection";

	public static final String CATALOG_COLLECTIONS = "catalog_collections";

	private CacheNames() {
	}
}
