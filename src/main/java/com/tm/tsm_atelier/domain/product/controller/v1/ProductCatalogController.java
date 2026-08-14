package com.tm.tsm_atelier.domain.product.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.product.dto.CareAxisOptionsDTO;
import com.tm.tsm_atelier.domain.product.dto.MaterialOptionDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class ProductCatalogController {

	/**
	 * O que a vitrine oferece para ordenar. A rota e publica e o
	 * {@code @PageableDefault} so define o padrao — sem esta lista, o cliente
	 * escolhia qualquer propriedade da entidade pela query string.
	 *
	 * <p>
	 * Duas consequencias, e as duas foram vistas: um campo inexistente virava
	 * {@code PropertyReferenceException} e saia como <strong>500</strong>, erro do
	 * cliente reportado como falha do servidor; e o {@code Pageable} inteiro entra
	 * na chave do cache {@code catalog_products}, entao um eixo de ordenacao aberto
	 * era um espaco de chaves aberto numa rota que ninguem precisa autenticar para
	 * chamar.
	 */
	private static final Set<String> SORTABLE_FIELDS = Set.of("name", "price", "promotionalPrice", "createdAt");

	private final ProductService productService;

	@GetMapping
	public ResponseEntity<Page<ProductSummaryDTO>> search(@ModelAttribute ProductSearchFilter filter,
			@PageableDefault(size = 12) Pageable pageable) {

		return ResponseEntity
				.ok(productService.searchCatalog(filter, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}

	/**
	 * Consulta pelo slug, e não por um id arrancado do texto. A versão anterior lia
	 * o último segmento como id: o produto era carregado por ele, e o slug só
	 * decidia se a resposta era 200 ou 301.
	 *
	 * <p>
	 * Isso quebrava tudo que não viesse do seed, porque o gerador monta o slug a
	 * partir do <em>nome</em> e só acrescenta número quando há colisão —
	 * {@code blusa-elegante} não termina em número, e o parse devolvia 404 para a
	 * URL que a própria API tinha acabado de entregar. Pior: o sufixo de
	 * desambiguação é indistinguível de um id, então o segundo "Blusa" virava
	 * {@code blusa-1} e a rota servia o <strong>produto 1</strong>, com 301 — que
	 * browser e buscador guardam para sempre.
	 *
	 * <p>
	 * Sem 301 agora: o slug é congelado na criação e o rename não o altera, então
	 * não existe versão anterior para onde redirecionar. Existe, 200; não existe,
	 * 404.
	 */
	@GetMapping("/slug/{slug}")
	public ResponseEntity<ProductResponseDTO> findBySlug(@PathVariable String slug) {
		return ResponseEntity.ok(productService.findBySlug(slug));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.findById(id));
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getCategories(@RequestParam(required = false) TargetAudience targetAudience) {
		return ResponseEntity.ok(Category.getByTargetAudience(targetAudience));
	}

	/**
	 * Vizinho de {@code /categories} porque e a mesma pergunta: quais valores este
	 * campo aceita. A diferenca e que material vem com rotulo — a constante nao e
	 * apresentavel, e traduzi-la no cliente recriaria a lista num segundo lugar.
	 */
	@GetMapping("/materials")
	public ResponseEntity<List<MaterialOptionDTO>> getMaterials() {
		return ResponseEntity.ok(Arrays.stream(Material.values()).map(MaterialOptionDTO::from).toList());
	}

	/**
	 * Sai agrupado por eixo, e nao como lista plana: um multi-select de dezesseis
	 * opcoes deixa o admin marcar "Nao lavar" junto com "Lavar a mao", e a etiqueta
	 * contraditoria e um erro pior que o de digitacao — passa despercebida.
	 */
	@GetMapping("/care-instructions")
	public ResponseEntity<List<CareAxisOptionsDTO>> getCareInstructions() {
		return ResponseEntity.ok(Arrays.stream(CareAxis.values()).map(CareAxisOptionsDTO::from).toList());
	}
}
