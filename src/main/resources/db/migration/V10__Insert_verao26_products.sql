-- Migration V10: Produtos da Coleção Verão 26

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(22, 'CAMISA DE POPELINA COM ALAMARES', 'camisa-de-popelina-com-alamares-22', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa De Popelina Com Alamares é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (22, 'Viscose', 90), (22, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(32, 22, 'Variante 2', '#A9A9A9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_capa_ei9p3z.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_hover_mkvoes.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (132, 32, 'PP', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-PP-COLOR2', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (133, 32, 'P', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-P-COLOR2', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (134, 32, 'M', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-M-COLOR2', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (135, 32, 'G', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-G-COLOR2', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(33, 22, 'Estampa Principal', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_COLOR2_FOTO1_ev3oqf.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_COLOR2_FOTO2_g5sgb0.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (136, 33, 'PP', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-PP', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (137, 33, 'P', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-P', 1);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (138, 33, 'M', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-M', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (139, 33, 'G', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-G', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (140, 33, 'GG', 'SKU-CAMISA-DE-POPELINA-COM-ALAMARES-22-GG', 18);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(23, 'COLETE COM BOTÕES E LINHO', 'colete-com-botoes-e-linho-23', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Colete Com Botões E Linho é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (23, 'Viscose', 90), (23, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(34, 23, 'Estampa Principal', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_capa_uz18wv.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_hover_uzxbzu.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (34, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_3_pc3wgq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (141, 34, 'PP', 'SKU-COLETE-COM-BOTOES-E-LINHO-23-PP', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (142, 34, 'P', 'SKU-COLETE-COM-BOTOES-E-LINHO-23-P', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (143, 34, 'M', 'SKU-COLETE-COM-BOTOES-E-LINHO-23-M', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (144, 34, 'G', 'SKU-COLETE-COM-BOTOES-E-LINHO-23-G', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (145, 34, 'GG', 'SKU-COLETE-COM-BOTOES-E-LINHO-23-GG', 7);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(24, 'TOP CURTO BORDADO FLORES', 'top-curto-bordado-flores-24', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Curto Bordado Flores é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (24, 'Viscose', 90), (24, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(35, 24, 'Estampa Principal', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457872/TOP_CURTO_BORDADO_FLORES_capa_jwuxcz.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_CURTO_BORDADO_FLORES_hover_uyo0qj.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (35, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457871/TOP_CURTO_BORDADO_FLORES_3_isymw1.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (146, 35, 'P', 'SKU-TOP-CURTO-BORDADO-FLORES-24-P', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (147, 35, 'M', 'SKU-TOP-CURTO-BORDADO-FLORES-24-M', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (148, 35, 'G', 'SKU-TOP-CURTO-BORDADO-FLORES-24-G', 26);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (149, 35, 'GG', 'SKU-TOP-CURTO-BORDADO-FLORES-24-GG', 27);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(25, 'TOP DE MALHA HALTER', 'top-de-malha-halter-25', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top De Malha Halter é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (25, 'Viscose', 90), (25, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(36, 25, 'Variante 2', '#A9A9A9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_capa_rcrrnj.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_capa_rcrrnj.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (150, 36, 'PP', 'SKU-TOP-DE-MALHA-HALTER-25-PP-COLOR2', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (151, 36, 'P', 'SKU-TOP-DE-MALHA-HALTER-25-P-COLOR2', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (152, 36, 'G', 'SKU-TOP-DE-MALHA-HALTER-25-G-COLOR2', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(37, 25, 'Variante 3', '#A9A9A9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_COLOR2_FOTO1_yf02k4.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457874/TOP_DE_MALHA_HALTER_COLOR2_FOTO2_n2hpa8.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (153, 37, 'PP', 'SKU-TOP-DE-MALHA-HALTER-25-PP-COLOR3', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (154, 37, 'P', 'SKU-TOP-DE-MALHA-HALTER-25-P-COLOR3', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (155, 37, 'M', 'SKU-TOP-DE-MALHA-HALTER-25-M-COLOR3', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (156, 37, 'GG', 'SKU-TOP-DE-MALHA-HALTER-25-GG-COLOR3', 18);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(38, 25, 'Variante 4', '#A9A9A9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457875/TOP_DE_MALHA_HALTER_COLOR3_FOTO1_pxwzma.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457875/TOP_DE_MALHA_HALTER_COLOR3_FOTO2_bo0jhd.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (157, 38, 'M', 'SKU-TOP-DE-MALHA-HALTER-25-M-COLOR4', 28);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (158, 38, 'G', 'SKU-TOP-DE-MALHA-HALTER-25-G-COLOR4', 1);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (159, 38, 'GG', 'SKU-TOP-DE-MALHA-HALTER-25-GG-COLOR4', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(39, 25, 'Estampa Principal', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457876/TOP_DE_MALHA_HALTER_COLOR4_FOTO1_bjch0e.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457876/TOP_DE_MALHA_HALTER_COLOR4_FOTO2_a0z7w7.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (160, 39, 'PP', 'SKU-TOP-DE-MALHA-HALTER-25-PP', 27);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (161, 39, 'P', 'SKU-TOP-DE-MALHA-HALTER-25-P', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (162, 39, 'M', 'SKU-TOP-DE-MALHA-HALTER-25-M', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (163, 39, 'G', 'SKU-TOP-DE-MALHA-HALTER-25-G', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (164, 39, 'GG', 'SKU-TOP-DE-MALHA-HALTER-25-GG', 15);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(26, 'TOP LISTRADO COM ALAMARES', 'top-listrado-com-alamares-26', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Listrado Com Alamares é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (26, 'Viscose', 90), (26, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(40, 26, 'Estampa Principal', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457879/TOP_LISTRADO_COM_ALAMARES_capa_epxl31.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457880/TOP_LISTRADO_COM_ALAMARES_hover_ztsirm.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (40, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457879/TOP_LISTRADO_COM_ALAMARES_3_wewxla.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (165, 40, 'PP', 'SKU-TOP-LISTRADO-COM-ALAMARES-26-PP', 28);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (166, 40, 'G', 'SKU-TOP-LISTRADO-COM-ALAMARES-26-G', 26);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));
