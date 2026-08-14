CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    promotional_price DECIMAL(10, 2),
    collection_id BIGINT,
    category VARCHAR(50) NOT NULL,
    target_audience VARCHAR(50) NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_collection FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL,
    CONSTRAINT ck_products_promotional_price
        CHECK (promotional_price IS NULL OR (promotional_price > 0 AND promotional_price < price))
);

-- material guarda o nome de uma constante do enum Material, e nao texto digitado
-- -- por isso 30 e nao 255. A coluna e metade da chave primaria: enquanto foi
-- texto livre, "Algodao" e "Algodão" eram dois materiais distintos no mesmo
-- produto, somavam 100% e passavam pela validacao de percentual.
CREATE TABLE product_fabric_compositions (
    product_id BIGINT NOT NULL,
    material VARCHAR(30) NOT NULL,
    percentage INT NOT NULL,
    CONSTRAINT pk_product_fabric_compositions PRIMARY KEY (product_id, material),
    CONSTRAINT fk_fabric_composition_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- instruction guarda o nome de uma constante do enum CareInstruction, pelo mesmo
-- motivo de material acima: com texto livre a etiqueta era digitada peca a peca,
-- e "Lavar a mao" e "Lavar à mão" eram duas instrucoes distintas para a chave
-- primaria. O enum tambem carrega o eixo de cada instrucao, e e o eixo que
-- permite recusar uma etiqueta contraditoria -- "Nao lavar" junto de "Lavar a
-- mao" -- antes de a peca ir para a vitrine.
CREATE TABLE product_care_instructions (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    instruction VARCHAR(40) NOT NULL,
    CONSTRAINT pk_product_care_instructions PRIMARY KEY (product_id, instruction)
);

CREATE TABLE product_colors (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    color_name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(10) NOT NULL,
    cover_image_url VARCHAR(500),
    hover_image_url VARCHAR(500),
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_gallery_images (
    product_color_id BIGINT NOT NULL REFERENCES product_colors(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT pk_product_gallery_images PRIMARY KEY (product_color_id, image_url)
);

-- De onde sai o sku_code. Ele e interno -- nao vem de ERP nem vai para etiqueta
-- impressa --, entao o admin nao o digita mais: o ProductService pede um numero
-- daqui e monta TSM-000123.
--
-- Sequencia, e nao valor derivado do produto, porque o codigo e copiado para
-- dentro do pedido no checkout e a partir dali e imutavel -- e tudo que a
-- aplicacao conhece no instante do insert (nome do produto, nome da cor,
-- tamanho) pode ser editado depois. Um codigo descritivo viraria uma afirmacao
-- falsa no primeiro rename, gravada aqui e repetida em todo pedido antigo.
--
-- Sequencia, e nao aleatorio, porque e unica por construcao: dispensa consulta
-- de verificacao e laco de retentativa. E como numero nunca se repete, um SKU
-- removido e restaurado nao colide com ninguem.
CREATE SEQUENCE sku_code_seq START WITH 1;

CREATE TABLE product_skus (
    id BIGSERIAL PRIMARY KEY,
    product_color_id BIGINT NOT NULL REFERENCES product_colors(id) ON DELETE CASCADE,
    size VARCHAR(2) NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP,
    -- Trava otimista do SKU. Protege gravações concorrentes na mesma linha, e é o
    -- que a contagem de inventário do PATCH de estoque compara para recusar um
    -- valor absoluto contado contra uma leitura já vencida.
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rede de ultima linha, e nao mais o que separa dois admins disputando um
-- codigo: ninguem digita sku_code desde que ele passou a sair da sequencia.
-- Fica porque este arquivo nao e o unico caminho ate a tabela -- o seed do V9
-- escreve direto, sem passar pelo ProductService.
--
-- Parcial de proposito: SKU removido libera o codigo. Nao ha mais quem o
-- reutilize pela API, mas tornar o indice total mudaria o que a restauracao
-- enxerga, e nao ha motivo para mexer nisso agora.
CREATE UNIQUE INDEX idx_sku_code_active ON product_skus (sku_code) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_collection_id ON products (collection_id);

CREATE INDEX idx_products_effective_price ON products (COALESCE(promotional_price, price));

-- Toda busca da vitrine comeca por "deleted_at IS NULL AND active", que e o que
-- ProductSpecification.isNotDeleted().and(isActive()) monta -- e nenhum dos
-- indices acima cobre esse filtro. Parcial em vez de composto: o predicado e
-- constante em toda consulta publica, entao ele cabe no WHERE do indice e o que
-- sobra e uma lista so dos produtos publicaveis, menor que a tabela.
--
-- Nao ajuda a busca textual: o LIKE '%termo%' de search() tem curinga a
-- esquerda e nenhum B-tree o atende. Se o catalogo crescer, o caminho e pg_trgm
-- com indice GIN, que e outra decisao.
CREATE INDEX idx_products_catalog_visible ON products (id) WHERE deleted_at IS NULL AND active;

CREATE INDEX idx_product_colors_product_id ON product_colors (product_id);
CREATE INDEX idx_product_skus_color_id ON product_skus (product_color_id);
