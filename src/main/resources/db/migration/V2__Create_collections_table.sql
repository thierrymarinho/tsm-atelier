CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(255),
    display_position VARCHAR(50) DEFAULT 'NONE',
    display_order INT DEFAULT 0,
    target_audience VARCHAR(50) NOT NULL DEFAULT 'UNISEX',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE collections ADD CONSTRAINT uk_collection_name_audience UNIQUE (name, target_audience);
ALTER TABLE collections ADD CONSTRAINT uk_collection_slug UNIQUE (slug);

CREATE UNIQUE INDEX uk_one_home_featured
ON collections (display_position)
WHERE display_position = 'HOME_FEATURED';

CREATE UNIQUE INDEX uk_one_header_per_audience 
ON collections (target_audience) 
WHERE display_position = 'HEADER';
