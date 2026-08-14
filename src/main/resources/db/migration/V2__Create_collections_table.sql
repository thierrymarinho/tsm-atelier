CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    hero_image_url VARCHAR(255),
    portrait_image_url VARCHAR(255),
    square_image_url VARCHAR(255),
    display_position VARCHAR(50) DEFAULT 'NONE',
    display_order INT DEFAULT 0,
    target_audience VARCHAR(50) NOT NULL DEFAULT 'UNISEX',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

ALTER TABLE collections ADD CONSTRAINT uk_collection_name_audience UNIQUE (name, target_audience);
ALTER TABLE collections ADD CONSTRAINT uk_collection_slug UNIQUE (slug);

-- O deleted_at IS NULL nos tres indices nao e decoracao. A exclusao de colecao e
-- logica e nao limpa a posicao de destaque, e as checagens do CollectionService
-- sao JPQL -- o @SQLRestriction da entidade esconde delas a colecao removida.
-- Sem o filtro, uma colecao no lixo continuava ocupando o HOME_MAIN do site
-- inteiro: criar outra passava pelas checagens, quebrava no insert e voltava
-- como 409 "A data conflict occurred" apontando um registro invisivel em toda a
-- interface.
--
-- Com o filtro, os tres se alinham ao indice de sku_code: a posicao e liberada
-- na exclusao. O preco vai para o outro lado e esta pago em
-- CollectionService.restore -- a colecao restaurada volta com NONE, porque
-- alguem pode ter ocupado a posicao no intervalo.

CREATE UNIQUE INDEX uk_one_home_main
ON collections (display_position)
WHERE display_position = 'HOME_MAIN' AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_home_secondary_per_audience
ON collections (target_audience)
WHERE display_position = 'HOME_SECONDARY' AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_one_header_per_audience
ON collections (target_audience)
WHERE display_position = 'HEADER' AND deleted_at IS NULL;
