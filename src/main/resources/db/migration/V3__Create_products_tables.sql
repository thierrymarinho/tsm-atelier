CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    collection_id BIGINT,
    category VARCHAR(50) NOT NULL,
    target_audience VARCHAR(50) NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_collection FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL
);

CREATE TABLE product_fabric_compositions (
    product_id BIGINT NOT NULL,
    material VARCHAR(255) NOT NULL,
    percentage INT NOT NULL,
    CONSTRAINT pk_product_fabric_compositions PRIMARY KEY (product_id, material),
    CONSTRAINT fk_fabric_composition_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_care_instructions (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    instruction VARCHAR(255) NOT NULL,
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

CREATE TABLE product_skus (
    id BIGSERIAL PRIMARY KEY,
    product_color_id BIGINT NOT NULL REFERENCES product_colors(id) ON DELETE CASCADE,
    size VARCHAR(2) NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_sku_code_active ON product_skus (sku_code) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_collection_id ON products (collection_id);
CREATE INDEX idx_product_colors_product_id ON product_colors (product_id);
CREATE INDEX idx_product_skus_color_id ON product_skus (product_color_id);
