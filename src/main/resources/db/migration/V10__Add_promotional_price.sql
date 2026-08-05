-- Preço promocional opcional. Nulo significa "sem promoção": é assim que o
-- admin retira um produto da promoção, mandando o update sem o campo.
ALTER TABLE products ADD COLUMN promotional_price DECIMAL(10, 2);

-- A regra vive no banco, não só na aplicação. Sem esta constraint, um UPDATE
-- manual poderia cadastrar uma "promoção" que aumenta o preço, e o catálogo
-- passaria a exibir um desconto negativo.
ALTER TABLE products ADD CONSTRAINT ck_products_promotional_price
    CHECK (promotional_price IS NULL OR (promotional_price > 0 AND promotional_price < price));

-- O filtro e a ordenação por preço passam a usar COALESCE(promotional_price,
-- price). Sem um índice sobre a mesma expressão, toda busca por faixa de preço
-- varreria a tabela inteira.
CREATE INDEX idx_products_effective_price ON products (COALESCE(promotional_price, price));

-- O pedido congela os dois preços. Guardando apenas o cobrado, o desconto
-- desapareceria do histórico e não haveria como mostrar "de X por Y" depois.
ALTER TABLE order_items ADD COLUMN list_price_at_purchase DECIMAL(10, 2);

-- Pedidos anteriores à promoção foram cobrados pelo preço de tabela, então os
-- dois valores coincidem. Preencher agora permite deixar a coluna obrigatória e
-- poupa o resto do código de tratar nulo.
UPDATE order_items SET list_price_at_purchase = price_at_purchase WHERE list_price_at_purchase IS NULL;

ALTER TABLE order_items ALTER COLUMN list_price_at_purchase SET NOT NULL;
