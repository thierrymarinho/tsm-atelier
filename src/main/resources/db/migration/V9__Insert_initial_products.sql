
-- ======================================================================
-- Catálogo inicial da loja.
--
-- Depende de V8: os produtos referenciam collection_id, e as coleções
-- precisam existir antes.
--
-- Os ids são explícitos para as cores, os SKUs e as imagens poderem apontar
-- uns para os outros dentro deste arquivo; os setval ao fim de cada bloco
-- devolvem as sequences ao lugar, sem o que o primeiro produto cadastrado
-- pelo painel colidiria com um id já ocupado.
--
-- O slug vem só do nome, sem o id no fim -- é o mesmo formato que o
-- generateUniqueSlug do ProductService produz, e a rota pública consulta por
-- ele. Enquanto o slug daqui carregava o id, a rota extraía esse número do
-- texto e buscava por id: os produtos deste arquivo funcionavam e nenhum
-- cadastrado pelo painel funcionava.
-- ======================================================================

-- Vestidos
INSERT INTO products (id, name, slug, description, price, promotional_price, category, target_audience, collection_id, is_featured, active) VALUES
    (1, 'CAMISA VESTIDO', 'camisa-vestido', 'Descubra a elegância do Camisa Vestido. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 180.90,'DRESSES', 'WOMEN', null,true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (1, 'VISCOSE', 90), (1, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (1, 1, 'Marrom Escuro', '#221713', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_capa_zphawr.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_3_gvl2ty.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (1, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_hover_enmite.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (1, 1, 'PP', 'TSM-000001', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (2, 1, 'P', 'TSM-000002', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (3, 1, 'M', 'TSM-000003', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (4, 1, 'G', 'TSM-000004', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (5, 1, 'GG', 'TSM-000005', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
        (2, 'VESTIDO DRAPEADO GOLA ALTA', 'vestido-drapeado-gola-alta', 'Descubra a elegância do Vestido Drapeado Gola Alta. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (2, 'VISCOSE', 90), (2, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (2, 2, 'Preto', '#010100', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_capa_rj3lpg.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_DRAPEADO_GOLA_ALTA_hover_ipsrzx.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (2, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_3_dovada.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (2, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_4_khy0b5.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (6, 2, 'PP', 'TSM-000006', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (7, 2, 'P', 'TSM-000007', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (8, 2, 'M', 'TSM-000008', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (9, 2, 'G', 'TSM-000009', 8);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (3, 2, 'Cinza Chumbo', '#525158', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_color2_foto1_ueua6w.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_color2_foto2_tbumee.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (10, 3, 'P', 'TSM-000010', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (11, 3, 'M', 'TSM-000011', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (12, 3, 'G', 'TSM-000012', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (13, 3, 'GG', 'TSM-000013', 4);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (3, 'VESTIDO FLUIDO HALTER COM VOLUME', 'vestido-fluido-halter-com-volume', 'Descubra a elegância do Vestido Fluido Halter Com Volume. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.',299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (3, 'VISCOSE', 90), (3, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (4, 3, 'Preto', '#111012', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_FLUIDO_HALTER_COM_VOLUME_capa_ccvdsy.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_FLUIDO_HALTER_COM_VOLUME_hover_hfp3ob.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (4, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_FLUIDO_HALTER_COM_VOLUME_3_lxbrry.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (14, 4, 'PP', 'TSM-000014', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (15, 4, 'P', 'TSM-000015', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (16, 4, 'M', 'TSM-000016', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (17, 4, 'G', 'TSM-000017', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (4, 'VESTIDO LONGO DRAPEADO', 'vestido-longo-drapeado', 'Descubra a elegância do Vestido Longo Drapeado. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (4, 'VISCOSE', 90), (4, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (5, 4, 'Vermelho', '#C61632', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_LONGO_DRAPEADO_capa_ngpuvz.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_LONGO_DRAPEADO_hover_gcazx1.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (18, 5, 'P', 'TSM-000018', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (19, 5, 'M', 'TSM-000019', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (20, 5, 'G', 'TSM-000020', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (21, 5, 'GG', 'TSM-000021', 8);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (5, 'VESTIDO MIDI ACETINADO', 'vestido-midi-acetinado', 'Descubra a elegância do Vestido Midi Acetinado. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (5, 'VISCOSE', 90), (5, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (6, 5, 'Vinho', '#311B1D', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_capa_kov55o.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_3_qyv8rg.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (6, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_ACETINADO_hover_qvejax.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (22, 6, 'PP', 'TSM-000022', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (23, 6, 'P', 'TSM-000023', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (24, 6, 'M', 'TSM-000024', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (25, 6, 'G', 'TSM-000025', 9);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (7, 5, 'Azul Índigo', '#464F87', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_color2_foto1_esb2rf.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_color2_foto2_f8kvbn.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (7, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_ACETINADO_color2_foto3_q3eb9j.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (26, 7, 'P', 'TSM-000026', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (27, 7, 'M', 'TSM-000027', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (28, 7, 'G', 'TSM-000028', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (29, 7, 'GG', 'TSM-000029', 5);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (6, 'VESTIDO MIDI DE POPELINA COM GODÊS', 'vestido-midi-de-popelina-com-godes', 'Descubra a elegância do Vestido Midi De Popelina Com Godês. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.',299.90, 'DRESSES', 'WOMEN', true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (6, 'VISCOSE', 90), (6, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (8, 6, 'Cinza Claro', '#D6CCCD', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_capa_bohpxi.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_hover_q79psr.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (8, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_4_s6n5mh.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (8, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_3_jinmfp.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (30, 8, 'PP', 'TSM-000030', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (31, 8, 'P', 'TSM-000031', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (32, 8, 'M', 'TSM-000032', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (33, 8, 'G', 'TSM-000033', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (34, 8, 'GG', 'TSM-000034', 6);

-- Camisas masculinas

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (7, 'CAMISA ACTIVE', 'camisa-active', 'Descubra a elegância do(a) Camisa Active. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (7, 'COTTON', 95), (7, 'ELASTANE', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (10, 7, 'Off-White', '#ECE9E9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_capa_m8cgsq.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_ACTIVE_hover_if8jjp.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (10, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_3_o7zsbh.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (35, 10, 'P', 'TSM-000035', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (36, 10, 'M', 'TSM-000036', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (37, 10, 'G', 'TSM-000037', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (38, 10, 'GG', 'TSM-000038', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (39, 10, 'XG', 'TSM-000039', 4);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (11, 7, 'Preto', '#2C2A2D', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color2_foto1_e3rvwt.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color2_foto2_rnjcoe.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (40, 11, 'P', 'TSM-000040', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (41, 11, 'M', 'TSM-000041', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (42, 11, 'G', 'TSM-000042', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (43, 11, 'GG', 'TSM-000043', 0);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (12, 7, 'Azul Claro', '#C2D7E9', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color3_foto1_xxjp9k.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756128/CAMISA_ACTIVE_color3_foto3_x1wl5g.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (12, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color3_foto2_t0qme2.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (44, 12, 'PP', 'TSM-000044', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (45, 12, 'P', 'TSM-000045', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (46, 12, 'M', 'TSM-000046', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (47, 12, 'G', 'TSM-000047', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (13, 7, 'Grafite', '#413D36', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756128/CAMISA_ACTIVE_color4_foto1_pgut24.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_ACTIVE_color4_foto2_ekimhw.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (48, 13, 'P', 'TSM-000048', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (49, 13, 'M', 'TSM-000049', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (50, 13, 'G', 'TSM-000050', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (51, 13, 'GG', 'TSM-000051', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (52, 13, 'XG', 'TSM-000052', 2);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (8, 'CAMISA AMPLA REGULAR FIT EFEITO SUEDE', 'camisa-ampla-regular-fit-efeito-suede', 'Descubra a elegância do(a) Camisa Ampla Regular Fit Efeito Suede. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (8, 'COTTON', 95), (8, 'ELASTANE', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (14, 8, 'Marrom', '#635044', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756130/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_capa_wwfqbj.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_3_azb2ue.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (14, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_3_azb2ue.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (53, 14, 'P', 'TSM-000053', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (54, 14, 'M', 'TSM-000054', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (55, 14, 'G', 'TSM-000055', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (56, 14, 'GG', 'TSM-000056', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (9, 'CAMISA BOX FIT EM JEANS LEVE', 'camisa-box-fit-em-jeans-leve', 'Descubra a elegância do(a) Camisa Box Fit Em Jeans Leve. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (9, 'COTTON', 95), (9, 'ELASTANE', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (15, 9, 'Azul Gelo', '#C6D1DA', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756131/CAMISA_BOX_FIT_EM_JEANS_LEVE_capa_kpbq9q.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756131/CAMISA_BOX_FIT_EM_JEANS_LEVE_hover_jpguxd.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (15, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756130/CAMISA_BOX_FIT_EM_JEANS_LEVE_3_y1nwyu.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (57, 15, 'PP', 'TSM-000057', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (58, 15, 'P', 'TSM-000058', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (59, 15, 'M', 'TSM-000059', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (60, 15, 'G', 'TSM-000060', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (61, 15, 'GG', 'TSM-000061', 7);

-- Jaquetas e Casacos femininos

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (10, 'CASACO DE MOLETOM COM ZÍPER', 'casaco-de-moletom-com-ziper', 'Descubra a elegância do(a) Casaco De Moletom Com Zíper. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 349.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (10, 'COTTON', 80), (10, 'POLYESTER', 20);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (16, 10, 'Azul Gelo', '#DBE1E5', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_capa_wu8uyf.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_hover_u3jqgp.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (16, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_3_vnns9t.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (62, 16, 'PP', 'TSM-000062', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (63, 16, 'P', 'TSM-000063', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (64, 16, 'M', 'TSM-000064', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (65, 16, 'G', 'TSM-000065', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (66, 16, 'GG', 'TSM-000066', 0);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (17, 10, 'Marrom Escuro', '#453735', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto1_ig1vb4.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto2_ttmbit.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (17, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto3_m3cumt.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (67, 17, 'P', 'TSM-000067', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (68, 17, 'M', 'TSM-000068', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (69, 17, 'G', 'TSM-000069', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (70, 17, 'GG', 'TSM-000070', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (18, 10, 'Preto', '#050707', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto1_qmc0lw.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto2_zonogd.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (18, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto3_jsfo7o.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (71, 18, 'PP', 'TSM-000071', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (72, 18, 'P', 'TSM-000072', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (73, 18, 'M', 'TSM-000073', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (74, 18, 'G', 'TSM-000074', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (11, 'JAQUETA BOMBER COM PUNHOS EM CONTRASTE', 'jaqueta-bomber-com-punhos-em-contraste', 'Descubra a elegância do(a) Jaqueta Bomber Com Punhos Em Contraste. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 429.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (11, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (19, 11, 'Marrom Escuro', '#3B2A29', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_capa_dvm1bd.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_hover_rbt1ka.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (19, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_3_y3yiwk.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (19, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_4_hjpzm7.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (75, 19, 'PP', 'TSM-000075', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (76, 19, 'P', 'TSM-000076', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (77, 19, 'M', 'TSM-000077', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (78, 19, 'G', 'TSM-000078', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (79, 19, 'GG', 'TSM-000079', 3);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (20, 11, 'Preto', '#1A1A19', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_color2_foto1_ulbw7c.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831416/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_color2_foto2_g1omp0.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (80, 20, 'P', 'TSM-000080', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (81, 20, 'M', 'TSM-000081', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (82, 20, 'G', 'TSM-000082', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (83, 20, 'GG', 'TSM-000083', 7);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (12, 'JAQUETA CURTA ACOLCHOADA', 'jaqueta-curta-acolchoada', 'Descubra a elegância do(a) Jaqueta Curta Acolchoada. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 459.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (12, 'POLYAMIDE', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (21, 12, 'Preto', '#1A1820', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_capa_ca8wvs.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_hover_zgb6cx.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (21, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_3_aculcg.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (84, 21, 'PP', 'TSM-000084', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (85, 21, 'P', 'TSM-000085', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (86, 21, 'M', 'TSM-000086', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (87, 21, 'G', 'TSM-000087', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (22, 12, 'Marrom Escuro', '#3B2C28', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_color2_foto1_re1nbl.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_color2_foto2_yivjlz.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (22, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_color2_foto3_jtkth8.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (88, 22, 'P', 'TSM-000088', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (89, 22, 'M', 'TSM-000089', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (90, 22, 'G', 'TSM-000090', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (91, 22, 'GG', 'TSM-000091', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (13, 'JAQUETA DE POLIURETANO COM BOLSOS', 'jaqueta-de-poliuretano-com-bolsos', 'Descubra a elegância do(a) Jaqueta De Poliuretano Com Bolsos. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 499.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (13, 'POLYURETHANE', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (23, 13, 'Preto', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_capa_zop5d6.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_hover_siupa0.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (23, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_3_ilifv8.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (92, 23, 'PP', 'TSM-000092', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (93, 23, 'P', 'TSM-000093', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (94, 23, 'M', 'TSM-000094', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (95, 23, 'G', 'TSM-000095', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (96, 23, 'GG', 'TSM-000096', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (14, 'TRENCH CURTO CRUZADO', 'trench-curto-cruzado', 'Descubra a elegância do(a) Trench Curto Cruzado. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 549.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (14, 'COTTON', 65), (14, 'POLYESTER', 35);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (24, 14, 'Bege', '#C7B29E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_capa_eauecu.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831420/TRENCH_CURTO_CRUZADO_hover_cafs0v.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (24, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/TRENCH_CURTO_CRUZADO_3_wdvj2c.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (24, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/TRENCH_CURTO_CRUZADO_4_cuvudf.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (97, 24, 'PP', 'TSM-000097', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (98, 24, 'P', 'TSM-000098', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (99, 24, 'M', 'TSM-000099', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (100, 24, 'G', 'TSM-000100', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (101, 24, 'GG', 'TSM-000101', 2);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (15, 'TRENCH CURTO CRUZADO DE COURO', 'trench-curto-cruzado-de-couro', 'Descubra a elegância do(a) Trench Curto Cruzado De Couro. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 699.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (15, 'LEATHER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (25, 15, 'Bege', '#C7B29E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_DE_COURO_capa_ob8lw1.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831420/TRENCH_CURTO_CRUZADO_DE_COURO_hover_bokyrw.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (25, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_DE_COURO_3_odsvhe.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (102, 25, 'PP', 'TSM-000102', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (103, 25, 'P', 'TSM-000103', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (104, 25, 'M', 'TSM-000104', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (105, 25, 'G', 'TSM-000105', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (106, 25, 'GG', 'TSM-000106', 0);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos da Coleção Flores 26
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(16, 'Blazer Floral', 'blazer-floral', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Blazer Floral é perfeita para os dias quentes.', 299.90, 'JACKETS', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (16, 'VISCOSE', 90), (16, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(26, 16, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435600/BLZR_FLWRS_capa_gukoye.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435601/BLZR_FLWRS_hover_cxxues.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (26, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435599/BLZR_FLWRS_3_t7908e.jpg');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (26, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435600/BLZR_FLWRS_4_p3l8nd.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (107, 26, 'PP', 'TSM-000107', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (108, 26, 'P', 'TSM-000108', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (109, 26, 'M', 'TSM-000109', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (110, 26, 'GG', 'TSM-000110', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(17, 'Blusa Drapeada Floral', 'blusa-drapeada-floral', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Blusa Drapeada Floral é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (17, 'VISCOSE', 90), (17, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(27, 17, 'Estampa Floral', '#EEDDD5', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435603/DRPD_FLWRS_TP_capa_xipdbx.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435604/DRPD_FLWRS_TP_hover_jjoax4.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (27, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435602/DRPD_FLWRS_TP_3_wetssf.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (111, 27, 'P', 'TSM-000111', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (112, 27, 'G', 'TSM-000112', 29);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(18, 'Camisa Bordada Vazada', 'camisa-bordada-vazada', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa Bordada Vazada é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (18, 'VISCOSE', 90), (18, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(28, 18, 'Estampa Floral', '#EBE2DF', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435605/SCHFFL_SHRT_capa_pnz8y4.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435606/SCHFFL_SHRT_hover_ucibrz.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (28, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435604/SCHFFL_SHRT_3_jxaxqg.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (113, 28, 'PP', 'TSM-000113', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (114, 28, 'P', 'TSM-000114', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (115, 28, 'M', 'TSM-000115', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (116, 28, 'G', 'TSM-000116', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (117, 28, 'GG', 'TSM-000117', 24);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(19, 'Vestido Curto Floral', 'vestido-curto-floral', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Vestido Curto Floral é perfeita para os dias quentes.', 299.90, 'DRESSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (19, 'VISCOSE', 90), (19, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(29, 19, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435608/SHRT_FLWRS_DRSS_capa_o5okyi.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SHRT_FLWRS_DRSS_hover_v1up9a.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (29, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SLVLSS_FLWRS_SHRT_3_fa9fuq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (118, 29, 'P', 'TSM-000118', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (119, 29, 'M', 'TSM-000119', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (120, 29, 'G', 'TSM-000120', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (121, 29, 'GG', 'TSM-000121', 26);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(20, 'Camisa Floral Sem Mangas', 'camisa-floral-sem-mangas', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa Floral Sem Mangas é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (20, 'VISCOSE', 90), (20, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(30, 20, 'Estampa Floral', '#E7D7CA', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435610/SLVLSS_FLWRS_SHRT_capa_ufrbeh.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435611/SLVLSS_FLWRS_SHRT_hover_avwvbw.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (30, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435609/SLVLSS_FLWRS_SHRT_3_fa9fuq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (122, 30, 'PP', 'TSM-000122', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (123, 30, 'P', 'TSM-000123', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (124, 30, 'M', 'TSM-000124', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (125, 30, 'G', 'TSM-000125', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (126, 30, 'GG', 'TSM-000126', 22);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(21, 'Vestido Floral', 'vestido-floral', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Vestido Floral é perfeita para os dias quentes.', 299.90, 'DRESSES', 'WOMEN', 5, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (21, 'VISCOSE', 90), (21, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(31, 21, 'Estampa Floral', '#FFB6C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435615/TSM_FLOWERS_DRESS_capa_n4e0ib.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435616/TSM_FLOWERS_DRESS_hover_epsrrx.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (31, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435612/TSM_FLOWERS_DRESS_3_d75vtu.jpg');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (31, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785435613/TSM_FLOWERS_DRESS_4_p7qlqr.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (127, 31, 'PP', 'TSM-000127', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (128, 31, 'P', 'TSM-000128', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (129, 31, 'M', 'TSM-000129', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (130, 31, 'G', 'TSM-000130', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (131, 31, 'GG', 'TSM-000131', 0);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos da Coleção Verão 26
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(22, 'CAMISA DE POPELINA COM ALAMARES', 'camisa-de-popelina-com-alamares', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Camisa De Popelina Com Alamares é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 4, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (22, 'VISCOSE', 90), (22, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(32, 22, 'Cinza Claro', '#D3D2D4', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_capa_ei9p3z.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_hover_mkvoes.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (132, 32, 'PP', 'TSM-000132', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (133, 32, 'P', 'TSM-000133', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (134, 32, 'M', 'TSM-000134', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (135, 32, 'G', 'TSM-000135', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(33, 22, 'Azul Acinzentado', '#879BB6', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_COLOR2_FOTO1_ev3oqf.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457869/CAMISA_DE_POPELINA_COM_ALAMARES_COLOR2_FOTO2_g5sgb0.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (136, 33, 'PP', 'TSM-000136', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (137, 33, 'P', 'TSM-000137', 1);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (138, 33, 'M', 'TSM-000138', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (139, 33, 'G', 'TSM-000139', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (140, 33, 'GG', 'TSM-000140', 18);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(23, 'COLETE COM BOTÕES E LINHO', 'colete-com-botoes-e-linho', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Colete Com Botões E Linho é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (23, 'VISCOSE', 90), (23, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(34, 23, 'Branco', '#FEFEFF', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_capa_uz18wv.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_hover_uzxbzu.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (34, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457870/COLETE_COM_BOT%C3%95ES_E_LINHO_3_pc3wgq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (141, 34, 'PP', 'TSM-000141', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (142, 34, 'P', 'TSM-000142', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (143, 34, 'M', 'TSM-000143', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (144, 34, 'G', 'TSM-000144', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (145, 34, 'GG', 'TSM-000145', 7);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(24, 'TOP CURTO BORDADO FLORES', 'top-curto-bordado-flores', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Curto Bordado Flores é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (24, 'VISCOSE', 90), (24, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(35, 24, 'Azul Meia-Noite', '#14151F', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457872/TOP_CURTO_BORDADO_FLORES_capa_jwuxcz.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_CURTO_BORDADO_FLORES_hover_uyo0qj.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (35, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457871/TOP_CURTO_BORDADO_FLORES_3_isymw1.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (146, 35, 'P', 'TSM-000146', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (147, 35, 'M', 'TSM-000147', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (148, 35, 'G', 'TSM-000148', 26);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (149, 35, 'GG', 'TSM-000149', 27);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(25, 'TOP DE MALHA HALTER', 'top-de-malha-halter', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top De Malha Halter é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (25, 'VISCOSE', 90), (25, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(36, 25, 'Preto', '#0D0F14', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_capa_rcrrnj.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_capa_rcrrnj.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (150, 36, 'PP', 'TSM-000150', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (151, 36, 'P', 'TSM-000151', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (152, 36, 'G', 'TSM-000152', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(37, 25, 'Azul Meia-Noite', '#05121E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457873/TOP_DE_MALHA_HALTER_COLOR2_FOTO1_yf02k4.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457874/TOP_DE_MALHA_HALTER_COLOR2_FOTO2_n2hpa8.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (153, 37, 'PP', 'TSM-000153', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (154, 37, 'P', 'TSM-000154', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (155, 37, 'M', 'TSM-000155', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (156, 37, 'GG', 'TSM-000156', 18);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(38, 25, 'Marrom Escuro', '#382321', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457875/TOP_DE_MALHA_HALTER_COLOR3_FOTO1_pxwzma.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457875/TOP_DE_MALHA_HALTER_COLOR3_FOTO2_bo0jhd.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (157, 38, 'M', 'TSM-000157', 28);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (158, 38, 'G', 'TSM-000158', 1);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (159, 38, 'GG', 'TSM-000159', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(39, 25, 'Bege', '#D0C2B3', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457876/TOP_DE_MALHA_HALTER_COLOR4_FOTO1_bjch0e.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457876/TOP_DE_MALHA_HALTER_COLOR4_FOTO2_a0z7w7.jpg');


INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (160, 39, 'PP', 'TSM-000160', 27);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (161, 39, 'P', 'TSM-000161', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (162, 39, 'M', 'TSM-000162', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (163, 39, 'G', 'TSM-000163', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (164, 39, 'GG', 'TSM-000164', 15);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(26, 'TOP LISTRADO COM ALAMARES', 'top-listrado-com-alamares', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Listrado Com Alamares é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 4, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (26, 'VISCOSE', 90), (26, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(40, 26, 'Azul Claro', '#97C4E5', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457879/TOP_LISTRADO_COM_ALAMARES_capa_epxl31.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457880/TOP_LISTRADO_COM_ALAMARES_hover_ztsirm.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (40, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785457879/TOP_LISTRADO_COM_ALAMARES_3_wewxla.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (165, 40, 'PP', 'TSM-000165', 28);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (166, 40, 'G', 'TSM-000166', 26);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos da Coleção Inverno Elegante
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(27, 'CASACO COM LÃ', 'casaco-com-la', 'A peça Casaco Com Lã traz a essência do Inverno Elegante, proporcionando sofisticação e conforto térmico para os dias mais frios.', 499.90, 'COATS_AND_TRENCHES', 'WOMEN', 2, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (27, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(41, 27, 'Preto', '#1E2224', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529288/CASACO_COM_L%C3%83_capa_rc1nnr.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529289/CASACO_COM_L%C3%83_hover_nthefx.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (167, 41, 'P', 'TSM-000167', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (168, 41, 'M', 'TSM-000168', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (169, 41, 'G', 'TSM-000169', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(42, 27, 'Caramelo', '#BC916C', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529288/CASACO_COM_L%C3%83_COLOR2_FOTO1_gbzrvt.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529289/CASACO_COM_L%C3%83_COLOR2_FOTO2_hbggnh.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (170, 42, 'P', 'TSM-000170', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (171, 42, 'M', 'TSM-000171', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (172, 42, 'G', 'TSM-000172', 10);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(28, 'CASACO CURTO DE PELO SINTÉTICO', 'casaco-curto-de-pelo-sintetico', 'A peça Casaco Curto De Pelo Sintético traz a essência do Inverno Elegante, proporcionando sofisticação e conforto térmico para os dias mais frios.', 499.90, 'COATS_AND_TRENCHES', 'WOMEN', 2, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (28, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(43, 28, 'Marrom Escuro', '#3A241E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529291/CASACO_CURTO_DE_PELO_SINT%C3%89TICO_capa_sms0pa.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529291/CASACO_CURTO_DE_PELO_SINT%C3%89TICO_hover_xth7q4.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (173, 43, 'P', 'TSM-000173', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (174, 43, 'M', 'TSM-000174', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (175, 43, 'G', 'TSM-000175', 10);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(29, 'CASACO DE PELO SINTÉTICO', 'casaco-de-pelo-sintetico', 'A peça Casaco De Pelo Sintético traz a essência do Inverno Elegante, proporcionando sofisticação e conforto térmico para os dias mais frios.', 499.90, 'COATS_AND_TRENCHES', 'WOMEN', 2, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (29, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(44, 29, 'Bege', '#B3A897', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529291/CASACO_DE_PELO_SINT%C3%89TICO_capa_alkzdp.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529292/CASACO_DE_PELO_SINT%C3%89TICO_hover_ccdvyj.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (176, 44, 'P', 'TSM-000176', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (177, 44, 'M', 'TSM-000177', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (178, 44, 'G', 'TSM-000178', 10);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(30, 'TRENCH AMPLO COM CINTO', 'trench-amplo-com-cinto', 'A peça Trench Amplo Com Cinto traz a essência do Inverno Elegante, proporcionando sofisticação e conforto térmico para os dias mais frios.', 499.90, 'COATS_AND_TRENCHES', 'WOMEN', 2, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (30, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(45, 30, 'Preto', '#222327', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529295/TRENCH_AMPLO_COM_CINTO_capa_phkkmn.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529296/TRENCH_AMPLO_COM_CINTO_hover_ccn2fw.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (179, 45, 'P', 'TSM-000179', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (180, 45, 'M', 'TSM-000180', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (181, 45, 'G', 'TSM-000181', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(46, 30, 'Bege', '#C9AF94', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529295/TRENCH_AMPLO_COM_CINTO_COLOR2_FOTO1_fa9prb.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785529295/TRENCH_AMPLO_COM_CINTO_COLOR2_FOTO2_pvwwxf.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (182, 46, 'P', 'TSM-000182', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (183, 46, 'M', 'TSM-000183', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (184, 46, 'G', 'TSM-000184', 10);


SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Mais Produtos da Coleção Verão 26
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(31, 'TOP DE GASA COM ALÇAS BORDADAS', 'top-de-gasa-com-alcas-bordadas', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top De Gasa Com Alças Bordadas é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (31, 'VISCOSE', 90), (31, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(47, 31, 'Bege Claro', '#CECEC1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531865/TOP_DE_GASA_COM_AL%C3%87AS_BORDADAS_capa_tlln8a.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531866/TOP_DE_GASA_COM_AL%C3%87AS_BORDADAS_hover_pnuisn.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (47, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531865/TOP_DE_GASA_COM_AL%C3%87AS_BORDADAS_3_kti8us.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (185, 47, 'PP', 'TSM-000185', 32);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (186, 47, 'P', 'TSM-000186', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (187, 47, 'M', 'TSM-000187', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (188, 47, 'G', 'TSM-000188', 6);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(48, 31, 'Marrom Escuro', '#25110A', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531865/TOP_DE_GASA_COM_AL%C3%87AS_BORDADAS_COLOR2_FOTO1_dlznze.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531865/TOP_DE_GASA_COM_AL%C3%87AS_BORDADAS_COLOR2_FOTO2_kdfsid.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (189, 48, 'PP', 'TSM-000189', 32);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (190, 48, 'P', 'TSM-000190', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (191, 48, 'M', 'TSM-000191', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (192, 48, 'G', 'TSM-000192', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (193, 48, 'GG', 'TSM-000193', 14);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(32, 'TOP HALTER ACETINADO COM ESTAMPA ANIMAL', 'top-halter-acetinado-com-estampa-animal', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Halter Acetinado Com Estampa Animal é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (32, 'VISCOSE', 90), (32, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(49, 32, 'Marrom', '#825F48', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531870/TOP_HALTER_ACETINADO_COM_ESTAMPA_ANIMAL_capa_ufusil.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531870/TOP_HALTER_ACETINADO_COM_ESTAMPA_ANIMAL_hover_alkbla.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (49, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531869/TOP_HALTER_ACETINADO_COM_ESTAMPA_ANIMAL_3_ezpgwl.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (194, 49, 'PP', 'TSM-000194', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (195, 49, 'P', 'TSM-000195', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (196, 49, 'G', 'TSM-000196', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (197, 49, 'GG', 'TSM-000197', 32);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(33, 'TOP HALTER PLUMETI', 'top-halter-plumeti', 'Descubra a vibração e o frescor da nova Coleção Verão 26. A peça Top Halter Plumeti é perfeita para os dias quentes.', 299.90, 'SHIRTS_AND_BLOUSES', 'WOMEN', 1, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (33, 'VISCOSE', 90), (33, 'ELASTANE', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(50, 33, 'Bege', '#CBBA9B', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531874/TOP_HALTER_PLUMETI_capa_ktjmgu.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531878/TOP_HALTER_PLUMETI_hover_cgytcj.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (50, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531871/TOP_HALTER_PLUMETI_3_nmzvbu.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (198, 50, 'PP', 'TSM-000198', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (199, 50, 'P', 'TSM-000199', 32);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (200, 50, 'M', 'TSM-000200', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (201, 50, 'GG', 'TSM-000201', 16);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(51, 33, 'Grafite', '#292929', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531874/TOP_HALTER_PLUMETI_COLOR2_FOTO1_qthfcd.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531875/TOP_HALTER_PLUMETI_COLOR2_FOTO2_px110a.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (51, 'https://res.cloudinary.com/apgaq55g/image/upload/v1785531876/TOP_HALTER_PLUMETI_COLOR2_FOTO3_tgmgwh.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (202, 51, 'PP', 'TSM-000202', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (203, 51, 'P', 'TSM-000203', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (204, 51, 'M', 'TSM-000204', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (205, 51, 'G', 'TSM-000205', 17);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos da Coleção Modern Gentleman
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(34, 'BLAZER DE TERNO COM EFEITO LAVADO', 'blazer-de-terno-com-efeito-lavado', 'A peça BLAZER DE TERNO COM EFEITO LAVADO foi desenhada para o cavalheiro moderno, unindo tradição e estilo contemporâneo com tecidos de alta qualidade.', 699.90, 'BLAZERS', 'MEN', 11, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (34, 'WOOL', 70), (34, 'POLYESTER', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(52, 34, 'Grafite', '#312C29', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540547/BLAZER_DE_TERNO_COM_EFEITO_LAVADO_capa_nsc1t9.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540547/BLAZER_DE_TERNO_COM_EFEITO_LAVADO_hover_lmcqug.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (206, 52, 'P', 'TSM-000206', 35);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (207, 52, 'M', 'TSM-000207', 21);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (208, 52, 'GG', 'TSM-000208', 31);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (209, 52, 'XG', 'TSM-000209', 11);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(35, 'BLAZER DE TERNO COM ESTRUTURA ESPIGA COM LÃ', 'blazer-de-terno-com-estrutura-espiga-com-la', 'A peça BLAZER DE TERNO COM ESTRUTURA ESPIGA COM LÃ foi desenhada para o cavalheiro moderno, unindo tradição e estilo contemporâneo com tecidos de alta qualidade.', 699.90, 'BLAZERS', 'MEN', 11, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (35, 'WOOL', 70), (35, 'POLYESTER', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(53, 35, 'Grafite', '#3F3E3A', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540545/BLAZER_DE_TERNO_COM_ESTRUTURA_ESPIGA_COM_L%C3%83_capa_s867el.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540544/BLAZER_DE_TERNO_COM_ESTRUTURA_ESPIGA_COM_L%C3%83_hover_adrdwq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (210, 53, 'P', 'TSM-000210', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (211, 53, 'M', 'TSM-000211', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (212, 53, 'G', 'TSM-000212', 27);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (213, 53, 'XG', 'TSM-000213', 20);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(36, 'BLAZER DE TERNO COM LÃ', 'blazer-de-terno-com-la', 'A peça BLAZER DE TERNO COM LÃ foi desenhada para o cavalheiro moderno, unindo tradição e estilo contemporâneo com tecidos de alta qualidade.', 699.90, 'BLAZERS', 'MEN', 11, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (36, 'WOOL', 70), (36, 'POLYESTER', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(54, 36, 'Azul Chumbo', '#34363F', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540542/BLAZER_DE_TERNO_COM_L%C3%83_capa_sychgm.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540539/BLAZER_DE_TERNO_COM_L%C3%83_hover_kp6vv6.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (214, 54, 'P', 'TSM-000214', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (215, 54, 'M', 'TSM-000215', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (216, 54, 'G', 'TSM-000216', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (217, 54, 'GG', 'TSM-000217', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (218, 54, 'XG', 'TSM-000218', 24);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(55, 36, 'Cinza Escuro', '#493F40', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540541/BLAZER_DE_TERNO_COM_L%C3%83_COLOR2_FOTO1_b1hf61.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540540/BLAZER_DE_TERNO_COM_L%C3%83_COLOR2_FOTO2_gnzir5.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (219, 55, 'P', 'TSM-000219', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (220, 55, 'G', 'TSM-000220', 30);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (221, 55, 'XG', 'TSM-000221', 12);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(37, 'BLAZER REGULAR FIT ESTRUTURADO', 'blazer-regular-fit-estruturado', 'A peça BLAZER REGULAR FIT ESTRUTURADO foi desenhada para o cavalheiro moderno, unindo tradição e estilo contemporâneo com tecidos de alta qualidade.', 699.90, 'BLAZERS', 'MEN', 11, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (37, 'WOOL', 70), (37, 'POLYESTER', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(56, 37, 'Cinza Escuro', '#575854', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540538/BLAZER_REGULAR_FIT_ESTRUTURADO_capa_tcfc6a.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785540538/BLAZER_REGULAR_FIT_ESTRUTURADO_hover_koivzf.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (222, 56, 'P', 'TSM-000222', 27);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (223, 56, 'M', 'TSM-000223', 29);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (224, 56, 'G', 'TSM-000224', 11);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos Jeans
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(38, 'JEANS TAPERED CINTURA ALTA', 'jeans-tapered-cintura-alta', 'A peça JEANS TAPERED CINTURA ALTA foi desenhada para a mulher moderna, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 399.90, 'JEANS', 'WOMEN', 3, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (38, 'COTTON', 98), (38, 'ELASTANE', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(57, 38, 'Lavagem Escura', '#2b3c5a', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544883/JEANS_TAPERED_CINTURA_ALTA_capa_vxvym0.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544888/JEANS_TAPERED_CINTURA_ALTA_hover_yoteiq.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (225, 57, 'PP', 'TSM-000225', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (226, 57, 'P', 'TSM-000226', 21);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (227, 57, 'G', 'TSM-000227', 15);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(58, 38, 'Lavagem Clara', '#8ba8d2', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544884/JEANS_TAPERED_CINTURA_ALTA_color2_foto1_tmmydd.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544885/JEANS_TAPERED_CINTURA_ALTA_color2_foto2_wdcs7c.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (228, 58, 'PP', 'TSM-000228', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (229, 58, 'P', 'TSM-000229', 33);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (230, 58, 'M', 'TSM-000230', 12);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(59, 38, 'Azul Jeans', '#5E86C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544886/JEANS_TAPERED_CINTURA_ALTA_color3_foto1_ztpukz.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544887/JEANS_TAPERED_CINTURA_ALTA_color3_foto2_kxcufr.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (231, 59, 'PP', 'TSM-000231', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (232, 59, 'P', 'TSM-000232', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (233, 59, 'G', 'TSM-000233', 35);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (234, 59, 'GG', 'TSM-000234', 21);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(39, 'JEANS Z1975 BARREL CINTURA ALTA COSTURAS', 'jeans-z1975-barrel-cintura-alta-costuras', 'A peça JEANS Z1975 BARREL CINTURA ALTA COSTURAS foi desenhada para a mulher moderna, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 399.90, 'JEANS', 'WOMEN', 3, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (39, 'COTTON', 98), (39, 'ELASTANE', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(60, 39, 'Azul Jeans', '#5E86C1', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544889/JEANS_Z1975_BARREL_CINTURA_ALTA_COSTURAS_capa_itmywe.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785544890/JEANS_Z1975_BARREL_CINTURA_ALTA_COSTURAS_hover_seomnh.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (235, 60, 'PP', 'TSM-000235', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (236, 60, 'P', 'TSM-000236', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (237, 60, 'M', 'TSM-000237', 34);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (238, 60, 'GG', 'TSM-000238', 7);


SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos Camisetas
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(40, 'CAMISETA GOLA REDONDA', 'camiseta-gola-redonda', 'A peça CAMISETA GOLA REDONDA foi desenhada para a mulher moderna, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 129.90, 'T_SHIRTS', 'WOMEN', 3, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (40, 'COTTON', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(61, 40, 'Branco', '#FFFFFF', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587368/CAMISETA_GOLA_REDONDA_capa_zeadfy.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587369/CAMISETA_GOLA_REDONDA_hover_y5nrai.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (239, 61, 'PP', 'TSM-000239', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (240, 61, 'M', 'TSM-000240', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (241, 61, 'G', 'TSM-000241', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (242, 61, 'GG', 'TSM-000242', 16);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(62, 40, 'Preto', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587368/CAMISETA_GOLA_REDONDA_color2_foto1_qhuz9f.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587368/CAMISETA_GOLA_REDONDA_color2_foto2_dovc55.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (243, 62, 'P', 'TSM-000243', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (244, 62, 'M', 'TSM-000244', 33);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (245, 62, 'GG', 'TSM-000245', 27);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(63, 40, 'Cinza Mescla', '#808080', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587368/CAMISETA_GOLA_REDONDA_color3_foto1_i4l1b9.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587368/CAMISETA_GOLA_REDONDA_color3_foto2_jgdi6x.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (246, 63, 'PP', 'TSM-000246', 21);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (247, 63, 'P', 'TSM-000247', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (248, 63, 'M', 'TSM-000248', 21);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (249, 63, 'G', 'TSM-000249', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (250, 63, 'GG', 'TSM-000250', 16);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(41, 'CAMISETA LAVADA HENRI MATISSE', 'camiseta-lavada-henri-matisse', 'A peça CAMISETA LAVADA HENRI MATISSE foi desenhada para a mulher moderna, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 129.90, 'T_SHIRTS', 'WOMEN', 3, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (41, 'COTTON', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(64, 41, 'Azul Royal', '#0656A5', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587369/CAMISETA_LAVADA_HENRI_MATISSE_capa_knx96m.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587369/CAMISETA_LAVADA_HENRI_MATISSE_hover_jvffwr.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (251, 64, 'PP', 'TSM-000251', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (252, 64, 'M', 'TSM-000252', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (253, 64, 'G', 'TSM-000253', 29);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(42, 'CAMISETA LISTRADA DE ALGODÃO E LINHO', 'camiseta-listrada-de-algodao-e-linho', 'A peça CAMISETA LISTRADA DE ALGODÃO E LINHO foi desenhada para a mulher moderna, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 129.90, 'T_SHIRTS', 'WOMEN', 3, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (42, 'COTTON', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(65, 42, 'Azul Claro', '#9FB4D0', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587369/CAMISETA_LISTRADA_DE_ALGOD%C3%83O_E_LINHO_capa_lnhd2g.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587372/CAMISETA_LISTRADA_DE_ALGOD%C3%83O_E_LINHO_hover_kczv6m.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (254, 65, 'P', 'TSM-000254', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (255, 65, 'G', 'TSM-000255', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (256, 65, 'GG', 'TSM-000256', 26);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(66, 42, 'Rosa Pink', '#F55284', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587369/CAMISETA_LISTRADA_DE_ALGOD%C3%83O_E_LINHO_color2_foto1_ands1p.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785587372/CAMISETA_LISTRADA_DE_ALGOD%C3%83O_E_LINHO_color2_foto2_t3o7fl.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (257, 66, 'PP', 'TSM-000257', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (258, 66, 'P', 'TSM-000258', 22);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (259, 66, 'M', 'TSM-000259', 9);


SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Produtos Camisetas Masculinas
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(43, 'CAMISETA DE TRICÔ COM ESTRUTURA CHENILLE', 'camiseta-de-trico-com-estrutura-chenille', 'A peça CAMISETA DE TRICÔ COM ESTRUTURA CHENILLE foi desenhada para o homem moderno, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 129.90, 'SHIRTS', 'MEN', 8, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (43, 'COTTON', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(67, 43, 'Preto', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588153/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_capa_xj3kzc.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588155/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_hover_rrwkpa.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (260, 67, 'M', 'TSM-000260', 21);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (261, 67, 'G', 'TSM-000261', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (262, 67, 'GG', 'TSM-000262', 26);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (263, 67, 'XG', 'TSM-000263', 33);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(68, 43, 'Cinza Mescla', '#808080', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588154/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_COLOR2_FOTO1_y5lmzt.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588154/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_COLOR2_FOTO2_qkhtrw.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (264, 68, 'G', 'TSM-000264', 31);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (265, 68, 'GG', 'TSM-000265', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (266, 68, 'XG', 'TSM-000266', 30);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(69, 43, 'Branco', '#FFFFFF', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588154/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_COLOR3_FOTO1_fhr7ia.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588155/CAMISETA_DE_TRIC%C3%94_COM_ESTRUTURA_CHENILLE_COLOR3_FOTO2_pqwiyu.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (267, 69, 'M', 'TSM-000267', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (268, 69, 'G', 'TSM-000268', 28);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (269, 69, 'GG', 'TSM-000269', 13);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES 
(44, 'CAMISETA ESTONADA REGULAR FIT', 'camiseta-estonada-regular-fit', 'A peça CAMISETA ESTONADA REGULAR FIT foi desenhada para o homem moderno, garantindo estilo e conforto excepcionais, perfeitos para compor seu guarda-roupa essencial.', 129.90, 'SHIRTS', 'MEN', 8, true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (44, 'COTTON', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES 
(70, 44, 'Verde Petróleo', '#346155', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588156/CAMISETA_ESTONADA_REGULAR_FIT_capa_puny28.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785588157/CAMISETA_ESTONADA_REGULAR_FIT_hover_r1valp.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (270, 70, 'P', 'TSM-000270', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (271, 70, 'M', 'TSM-000271', 31);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (272, 70, 'G', 'TSM-000272', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (273, 70, 'GG', 'TSM-000273', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (274, 70, 'XG', 'TSM-000274', 23);

-- ======================================================================
-- Produtos masculinos faltantes
-- ======================================================================

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (45, 'BERMUDA REGULAR FIT CONFORTO', 'bermuda-regular-fit-conforto', 'Descubra a elegância do(a) BERMUDA REGULAR FIT CONFORTO. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 179.9, 'SHORTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (45, 'COTTON', 98), (45, 'ELASTANE', 2);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (71, 45, 'Caqui', '#C0A060', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/BERMUDA_REGULAR_FIT_CONFORTO_capa_lofqwa.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/BERMUDA_REGULAR_FIT_CONFORTO_hover_hu446r.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (275, 71, 'P', 'TSM-000275', 25);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (276, 71, 'M', 'TSM-000276', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (277, 71, 'GG', 'TSM-000277', 23);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (278, 71, 'XG', 'TSM-000278', 31);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (46, 'CAMISETA REGATA LEVE RUNNING', 'camiseta-regata-leve-running', 'Descubra a elegância do(a) CAMISETA REGATA LEVE RUNNING. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 99.9, 'T_SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (46, 'POLYESTER', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (72, 46, 'Branco', '#FFFFFF', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/CAMISETA_REGATA_LEVE_RUNNING_capa_kuk2op.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/CAMISETA_REGATA_LEVE_RUNNING_hover_ci2tsy.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (279, 72, 'P', 'TSM-000279', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (280, 72, 'GG', 'TSM-000280', 35);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (281, 72, 'XG', 'TSM-000281', 8);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (47, 'CASACO CRUZADO COM LÃ', 'casaco-cruzado-com-la', 'Descubra a elegância do(a) CASACO CRUZADO COM LÃ. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 549.9, 'COATS_AND_TRENCHES', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (47, 'WOOL', 60), (47, 'POLYESTER', 40);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (73, 47, 'Cinza Escuro', '#3A3A3A', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/CASACO_CRUZADO_COM_L%C3%83_capa_mkqwzj.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623621/CASACO_CRUZADO_COM_L%C3%83_hover_nf0hl8.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (282, 73, 'P', 'TSM-000282', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (283, 73, 'M', 'TSM-000283', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (284, 73, 'G', 'TSM-000284', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (285, 73, 'GG', 'TSM-000285', 35);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (48, 'JAQUETA BOMBER DE TRICÔ 100% LÃ', 'jaqueta-bomber-de-trico-100-la', 'Descubra a elegância do(a) JAQUETA BOMBER DE TRICÔ 100% LÃ. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 499.9, 'JACKETS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (48, 'WOOL', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (74, 48, 'Marrom', '#5C3A1E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623622/JAQUETA_BOMBER_DE_TRIC%C3%94_100_L%C3%83_capa_vsgzvf.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623622/JAQUETA_BOMBER_DE_TRIC%C3%94_100_L%C3%83_hover_szrh7k.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (286, 74, 'P', 'TSM-000286', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (287, 74, 'M', 'TSM-000287', 30);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (288, 74, 'G', 'TSM-000288', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (289, 74, 'GG', 'TSM-000289', 35);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (290, 74, 'XG', 'TSM-000290', 19);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (49, 'JEANS RELAXED FIT COM VINCO', 'jeans-relaxed-fit-com-vinco', 'Descubra a elegância do(a) JEANS RELAXED FIT COM VINCO. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.9, 'JEANS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (49, 'COTTON', 99), (49, 'ELASTANE', 1);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (75, 49, 'Lavagem Escura', '#1A1A2E', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623622/JEANS_RELAXED_FIT_COM_VINCO_capa_gbpynb.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1785623622/JEANS_RELAXED_FIT_COM_VINCO_hover_sdqzpa.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (291, 75, 'P', 'TSM-000291', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (292, 75, 'M', 'TSM-000292', 24);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (293, 75, 'G', 'TSM-000293', 29);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (294, 75, 'GG', 'TSM-000294', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (295, 75, 'XG', 'TSM-000295', 25);


SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));

-- A sequencia dos codigos comeca depois do ultimo do seed, como as outras.
--
-- Aqui o numero do codigo coincide com o id do SKU, por conveniencia de leitura
-- deste arquivo. Na aplicacao os dois sao independentes: a sequencia avanca
-- mesmo em transacao revertida, entao eles divergem no primeiro rollback.
SELECT setval('sku_code_seq', (SELECT MAX(id) FROM product_skus));


-- ======================================================================
-- Etiqueta de conservacao, derivada da fibra dominante de cada produto.
--
-- Derivada, e nao curada peca a peca: e conselho de lavagem plausivel sobre
-- roupa inventada, do mesmo tecido que o resto deste arquivo. Se um dia ele
-- virar molde de importacao real, os cuidados precisam ser revistos junto
-- com os precos e as descricoes.
--
-- A forma tambem e deliberada. Escrita a mao seriam ~230 INSERTs, e este
-- caminho nao passa pelo ProductService: a regra de uma instrucao por eixo
-- vive na validacao da aplicacao, e o banco nao tem como impo-la -- o eixo
-- nem e coluna. Um DO_NOT_WASH digitado ao lado de um HAND_WASH entraria
-- calado. Como tabela de regras, conferir o eixo e ler sete blocos curtos;
-- o que fecha o resto esta em CareInstructionSeedTest.
-- ======================================================================
INSERT INTO product_care_instructions (product_id, instruction)
SELECT dominant.product_id, care.instruction
FROM (SELECT DISTINCT ON (product_id) product_id, material
      FROM product_fabric_compositions
      ORDER BY product_id, percentage DESC) dominant
JOIN (VALUES
    -- Algodao aguenta secadora e ferro mais quente que o resto da lista.
    ('COTTON', 'MACHINE_WASH_WARM'),
    ('COTTON', 'DO_NOT_BLEACH'),
    ('COTTON', 'TUMBLE_DRY_LOW'),
    ('COTTON', 'IRON_MEDIUM'),

    -- La encolhe na maquina e deforma pendurada no varal.
    ('WOOL', 'HAND_WASH'),
    ('WOOL', 'DO_NOT_BLEACH'),
    ('WOOL', 'DO_NOT_TUMBLE_DRY'),
    ('WOOL', 'DRY_FLAT'),
    ('WOOL', 'IRON_LOW'),

    -- Viscose perde cor no sol e encolhe no calor.
    ('VISCOSE', 'MACHINE_WASH_COLD'),
    ('VISCOSE', 'DO_NOT_BLEACH'),
    ('VISCOSE', 'DO_NOT_TUMBLE_DRY'),
    ('VISCOSE', 'DRY_IN_SHADE'),
    ('VISCOSE', 'IRON_LOW'),

    ('POLYESTER', 'MACHINE_WASH_COLD'),
    ('POLYESTER', 'DO_NOT_BLEACH'),
    ('POLYESTER', 'TUMBLE_DRY_LOW'),
    ('POLYESTER', 'IRON_LOW'),

    ('POLYAMIDE', 'MACHINE_WASH_COLD'),
    ('POLYAMIDE', 'DO_NOT_BLEACH'),
    ('POLYAMIDE', 'DO_NOT_TUMBLE_DRY'),
    ('POLYAMIDE', 'LINE_DRY'),
    ('POLYAMIDE', 'IRON_LOW'),

    -- O par que justifica o vocabulario existir: couro exige lavagem a seco,
    -- e o couro sintetico -- que a etiqueta declara como poliuretano -- a
    -- proibe, porque o solvente dissolve o revestimento.
    ('LEATHER', 'DO_NOT_WASH'),
    ('LEATHER', 'DO_NOT_BLEACH'),
    ('LEATHER', 'DO_NOT_TUMBLE_DRY'),
    ('LEATHER', 'DO_NOT_IRON'),
    ('LEATHER', 'DRY_CLEAN'),

    ('POLYURETHANE', 'HAND_WASH'),
    ('POLYURETHANE', 'DO_NOT_BLEACH'),
    ('POLYURETHANE', 'DO_NOT_TUMBLE_DRY'),
    ('POLYURETHANE', 'LINE_DRY'),
    ('POLYURETHANE', 'DO_NOT_IRON'),
    ('POLYURETHANE', 'DO_NOT_DRY_CLEAN')
) AS care(material, instruction) ON care.material = dominant.material;
