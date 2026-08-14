package com.tm.tsm_atelier.domain.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O pedido congela dados do produto e do endereco no momento da compra,
 * copiando valor por valor. Se uma coluna de destino for mais estreita que a de
 * origem, o cadastro aceita o valor e o checkout quebra depois — longe da
 * causa, e so para quem tentar comprar aquele produto.
 *
 * Foi exatamente o que aconteceu: order_items.image_url era VARCHAR(255)
 * recebendo product_colors.cover_image_url, que e VARCHAR(500). Nao disparava
 * porque as URLs do seed tem ~130 caracteres. Este teste fixa a invariante em
 * vez do caso: destino nunca menor que origem.
 */
@SpringBootTest
@DisplayName("Order snapshot columns must fit the values they copy")
class OrderSnapshotColumnWidthTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private record ColumnPair(String sourceTable, String sourceColumn, String targetTable, String targetColumn) {
		@Override
		public String toString() {
			return sourceTable + "." + sourceColumn + " -> " + targetTable + "." + targetColumn;
		}
	}

	static List<ColumnPair> snapshotPairs() {
		return List.of(new ColumnPair("product_colors", "cover_image_url", "order_items", "image_url"),
				new ColumnPair("products", "name", "order_items", "product_name"),
				new ColumnPair("product_skus", "sku_code", "order_items", "sku_code"),
				new ColumnPair("product_colors", "color_name", "order_items", "color"),
				new ColumnPair("addresses", "street", "orders", "street"),
				new ColumnPair("addresses", "number", "orders", "number"),
				new ColumnPair("addresses", "complement", "orders", "complement"),
				new ColumnPair("addresses", "neighborhood", "orders", "neighborhood"),
				new ColumnPair("addresses", "city", "orders", "city"),
				new ColumnPair("addresses", "postal_code", "orders", "postal_code"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("snapshotPairs")
	void snapshotColumnMustNotBeNarrowerThanItsSource(ColumnPair pair) {
		int source = maxLength(pair.sourceTable(), pair.sourceColumn());
		int target = maxLength(pair.targetTable(), pair.targetColumn());

		assertThat(target).as("%s: um valor valido na origem nao caberia no destino", pair)
				.isGreaterThanOrEqualTo(source);
	}

	private int maxLength(String table, String column) {
		Integer length = jdbcTemplate.queryForObject("SELECT character_maximum_length FROM information_schema.columns "
				+ "WHERE table_name = ? AND column_name = ?", Integer.class, table, column);

		assertThat(length).as("coluna %s.%s nao encontrada ou sem limite de tamanho", table, column).isNotNull();
		return length;
	}
}
