-- Migration V8: Produtos da Coleção Flores 26

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(16, 'Blazer Floral', 'blazer-floral-16', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Blazer Floral é perfeita para os dias quentes.', 299.90, 'JACKETS', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (16, 'Viscose', 90), (16, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(26, 16, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435600/BLZR_FLWRS_capa_gukoye.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435601/BLZR_FLWRS_hover_cxxues.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (26, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435599/BLZR_FLWRS_3_t7908e.jpg');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (26, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435600/BLZR_FLWRS_4_p3l8nd.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (107, 26, 'PP', 'SKU-BLAZER-FLORAL-16-PP', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (108, 26, 'P', 'SKU-BLAZER-FLORAL-16-P', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (109, 26, 'M', 'SKU-BLAZER-FLORAL-16-M', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (110, 26, 'GG', 'SKU-BLAZER-FLORAL-16-GG', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(17, 'Blusa Drapeada Floral', 'blusa-drapeada-floral-17', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Blusa Drapeada Floral é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (17, 'Viscose', 90), (17, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(27, 17, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435603/DRPD_FLWRS_TP_capa_xipdbx.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435604/DRPD_FLWRS_TP_hover_jjoax4.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (27, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435602/DRPD_FLWRS_TP_3_wetssf.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (111, 27, 'P', 'SKU-BLUSA-DRAPEADA-FLORAL-17-G', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (112, 27, 'G', 'SKU-BLUSA-DRAPEADA-FLORAL-17-GG', 29);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(18, 'Camisa Bordada Vazada', 'camisa-bordada-vazada-18', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa Bordada Vazada é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (18, 'Viscose', 90), (18, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(28, 18, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435605/SCHFFL_SHRT_capa_pnz8y4.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435606/SCHFFL_SHRT_hover_ucibrz.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (28, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435604/SCHFFL_SHRT_3_jxaxqg.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (113, 28, 'PP', 'SKU-CAMISA-BORDADA-VAZADA-18-PP', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (114, 28, 'P', 'SKU-CAMISA-BORDADA-VAZADA-18-P', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (115, 28, 'M', 'SKU-CAMISA-BORDADA-VAZADA-18-M', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (116, 28, 'G', 'SKU-CAMISA-BORDADA-VAZADA-18-G', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (117, 28, 'GG', 'SKU-CAMISA-BORDADA-VAZADA-18-GG', 24);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(19, 'Vestido Curto Floral', 'vestido-curto-floral-19', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Vestido Curto Floral é perfeita para os dias quentes.', 299.90, 'DRESSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (19, 'Viscose', 90), (19, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(29, 19, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435608/SHRT_FLWRS_DRSS_capa_o5okyi.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SHRT_FLWRS_DRSS_hover_v1up9a.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (29, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SLVLSS_FLWRS_SHRT_3_fa9fuq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (118, 29, 'P', 'SKU-VESTIDO-CURTO-FLORAL-19-P', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (119, 29, 'M', 'SKU-VESTIDO-CURTO-FLORAL-19-M', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (120, 29, 'G', 'SKU-VESTIDO-CURTO-FLORAL-19-G', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (121, 29, 'GG', 'SKU-VESTIDO-CURTO-FLORAL-19-GG', 26);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(20, 'Camisa Floral Sem Mangas', 'camisa-floral-sem-mangas-20', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa Floral Sem Mangas é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (20, 'Viscose', 90), (20, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(30, 20, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435610/SLVLSS_FLWRS_SHRT_capa_ufrbeh.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435611/SLVLSS_FLWRS_SHRT_hover_avwvbw.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (30, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SLVLSS_FLWRS_SHRT_3_fa9fuq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (122, 30, 'PP', 'SKU-CAMISA-FLORAL-SEM-MANGAS-20-PP', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (123, 30, 'P', 'SKU-CAMISA-FLORAL-SEM-MANGAS-20-P', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (124, 30, 'M', 'SKU-CAMISA-FLORAL-SEM-MANGAS-20-M', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (125, 30, 'G', 'SKU-CAMISA-FLORAL-SEM-MANGAS-20-G', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (126, 30, 'GG', 'SKU-CAMISA-FLORAL-SEM-MANGAS-20-GG', 22);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(21, 'Vestido Floral', 'vestido-floral-21', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Vestido Floral é perfeita para os dias quentes.', 299.90, 'DRESSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (21, 'Viscose', 90), (21, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(31, 21, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435615/TSM_FLOWERS_DRESS_capa_n4e0ib.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435616/TSM_FLOWERS_DRESS_hover_epsrrx.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (31, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435612/TSM_FLOWERS_DRESS_3_d75vtu.jpg');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (31, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435613/TSM_FLOWERS_DRESS_4_p7qlqr.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (127, 31, 'PP', 'SKU-VESTIDO-FLORAL-21-PP', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (128, 31, 'P', 'SKU-VESTIDO-FLORAL-21-P', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (129, 31, 'M', 'SKU-VESTIDO-FLORAL-21-M', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (130, 31, 'G', 'SKU-VESTIDO-FLORAL-21-G', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (131, 31, 'GG', 'SKU-VESTIDO-FLORAL-21-GG', 0);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));
