-- Vestidos
INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (1, 'CAMISA VESTIDO', 'camisa-vestido-1', 'Descubra a elegância do Camisa Vestido. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', 1,true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (1, 'Viscose', 90), (1, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (1, 1, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_capa_zphawr.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_3_gvl2ty.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (1, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/CAMISA_VESTIDO_hover_enmite.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (1, 1, 'PP', 'SKU-CAMISA-VESTIDO-1-DEFAULT-PP', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (2, 1, 'P', 'SKU-CAMISA-VESTIDO-1-DEFAULT-P', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (3, 1, 'M', 'SKU-CAMISA-VESTIDO-1-DEFAULT-M', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (4, 1, 'G', 'SKU-CAMISA-VESTIDO-1-DEFAULT-G', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (5, 1, 'GG', 'SKU-CAMISA-VESTIDO-1-DEFAULT-GG', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
        (2, 'VESTIDO DRAPEADO GOLA ALTA', 'vestido-drapeado-gola-alta-2', 'Descubra a elegância do Vestido Drapeado Gola Alta. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (2, 'Viscose', 90), (2, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (2, 2, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_capa_rj3lpg.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_DRAPEADO_GOLA_ALTA_hover_ipsrzx.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (2, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_3_dovada.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (2, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_4_khy0b5.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (6, 2, 'PP', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-DEFAULT-PP', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (7, 2, 'P', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-DEFAULT-P', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (8, 2, 'M', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-DEFAULT-M', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (9, 2, 'G', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-DEFAULT-G', 8);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (3, 2, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_color2_foto1_ueua6w.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675607/VESTIDO_DRAPEADO_GOLA_ALTA_color2_foto2_tbumee.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (10, 3, 'P', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-COLOR2-P', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (11, 3, 'M', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-COLOR2-M', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (12, 3, 'G', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-COLOR2-G', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (13, 3, 'GG', 'SKU-VESTIDO-DRAPEADO-GOLA-ALTA-2-COLOR2-GG', 4);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (3, 'VESTIDO FLUIDO HALTER COM VOLUME', 'vestido-fluido-halter-com-volume-3', 'Descubra a elegância do Vestido Fluido Halter Com Volume. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.',299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (3, 'Viscose', 90), (3, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (4, 3, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_FLUIDO_HALTER_COM_VOLUME_capa_ccvdsy.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_FLUIDO_HALTER_COM_VOLUME_hover_hfp3ob.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (4, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675608/VESTIDO_FLUIDO_HALTER_COM_VOLUME_3_lxbrry.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (14, 4, 'PP', 'SKU-VESTIDO-FLUIDO-HALTER-COM-VOLUME-3-DEFAULT-PP', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (15, 4, 'P', 'SKU-VESTIDO-FLUIDO-HALTER-COM-VOLUME-3-DEFAULT-P', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (16, 4, 'M', 'SKU-VESTIDO-FLUIDO-HALTER-COM-VOLUME-3-DEFAULT-M', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (17, 4, 'G', 'SKU-VESTIDO-FLUIDO-HALTER-COM-VOLUME-3-DEFAULT-G', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (4, 'VESTIDO LONGO DRAPEADO', 'vestido-longo-drapeado-4', 'Descubra a elegância do Vestido Longo Drapeado. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (4, 'Viscose', 90), (4, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (5, 4, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_LONGO_DRAPEADO_capa_ngpuvz.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675609/VESTIDO_LONGO_DRAPEADO_hover_gcazx1.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (18, 5, 'P', 'SKU-VESTIDO-LONGO-DRAPEADO-4-DEFAULT-P', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (19, 5, 'M', 'SKU-VESTIDO-LONGO-DRAPEADO-4-DEFAULT-M', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (20, 5, 'G', 'SKU-VESTIDO-LONGO-DRAPEADO-4-DEFAULT-G', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (21, 5, 'GG', 'SKU-VESTIDO-LONGO-DRAPEADO-4-DEFAULT-GG', 8);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (5, 'VESTIDO MIDI ACETINADO', 'vestido-midi-acetinado-5', 'Descubra a elegância do Vestido Midi Acetinado. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 299.90, 'DRESSES', 'WOMEN', false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (5, 'Viscose', 90), (5, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (6, 5, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_capa_kov55o.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_3_qyv8rg.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (6, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_ACETINADO_hover_qvejax.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (22, 6, 'PP', 'SKU-VESTIDO-MIDI-ACETINADO-5-DEFAULT-PP', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (23, 6, 'P', 'SKU-VESTIDO-MIDI-ACETINADO-5-DEFAULT-P', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (24, 6, 'M', 'SKU-VESTIDO-MIDI-ACETINADO-5-DEFAULT-M', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (25, 6, 'G', 'SKU-VESTIDO-MIDI-ACETINADO-5-DEFAULT-G', 9);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (7, 5, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_color2_foto1_esb2rf.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675610/VESTIDO_MIDI_ACETINADO_color2_foto2_f8kvbn.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (7, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_ACETINADO_color2_foto3_q3eb9j.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (26, 7, 'P', 'SKU-VESTIDO-MIDI-ACETINADO-5-COLOR2-P', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (27, 7, 'M', 'SKU-VESTIDO-MIDI-ACETINADO-5-COLOR2-M', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (28, 7, 'G', 'SKU-VESTIDO-MIDI-ACETINADO-5-COLOR2-G', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (29, 7, 'GG', 'SKU-VESTIDO-MIDI-ACETINADO-5-COLOR2-GG', 5);

INSERT INTO products (id, name, slug, description, price, category, target_audience, is_featured, active) VALUES
    (6, 'VESTIDO MIDI DE POPELINA COM GODÊS', 'vestido-midi-de-popelina-com-godes-6', 'Descubra a elegância do Vestido Midi De Popelina Com Godês. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.',299.90, 'DRESSES', 'WOMEN', true, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (6, 'Viscose', 90), (6, 'Elastano', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (8, 6, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_capa_bohpxi.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_hover_q79psr.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (8, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675612/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_4_s6n5mh.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (8, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784675611/VESTIDO_MIDI_DE_POPELINA_COM_GOD%C3%8AS_3_jinmfp.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (30, 8, 'PP', 'SKU-VESTIDO-MIDI-DE-POPELINA-COM-GODES-6-DEFAULT-PP', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (31, 8, 'P', 'SKU-VESTIDO-MIDI-DE-POPELINA-COM-GODES-6-DEFAULT-P', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (32, 8, 'M', 'SKU-VESTIDO-MIDI-DE-POPELINA-COM-GODES-6-DEFAULT-M', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (33, 8, 'G', 'SKU-VESTIDO-MIDI-DE-POPELINA-COM-GODES-6-DEFAULT-G', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (34, 8, 'GG', 'SKU-VESTIDO-MIDI-DE-POPELINA-COM-GODES-6-DEFAULT-GG', 6);

-- Camisas masculinas

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (7, 'CAMISA ACTIVE', 'camisa-active-7', 'Descubra a elegância do(a) Camisa Active. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (7, 'Algodão', 95), (7, 'Elastano', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (10, 7, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_capa_m8cgsq.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_ACTIVE_hover_if8jjp.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (10, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_3_o7zsbh.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (35, 10, 'P', 'SKU-CAMISA-ACTIVE-7-DEFAULT-P', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (36, 10, 'M', 'SKU-CAMISA-ACTIVE-7-DEFAULT-M', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (37, 10, 'G', 'SKU-CAMISA-ACTIVE-7-DEFAULT-G', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (38, 10, 'GG', 'SKU-CAMISA-ACTIVE-7-DEFAULT-GG', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (39, 10, 'XG', 'SKU-CAMISA-ACTIVE-7-DEFAULT-XG', 4);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (11, 7, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color2_foto1_e3rvwt.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color2_foto2_rnjcoe.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (40, 11, 'P', 'SKU-CAMISA-ACTIVE-7-COLOR2-P', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (41, 11, 'M', 'SKU-CAMISA-ACTIVE-7-COLOR2-M', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (42, 11, 'G', 'SKU-CAMISA-ACTIVE-7-COLOR2-G', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (43, 11, 'GG', 'SKU-CAMISA-ACTIVE-7-COLOR2-GG', 0);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (12, 7, 'Variação 3', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color3_foto1_xxjp9k.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756128/CAMISA_ACTIVE_color3_foto3_x1wl5g.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (12, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756127/CAMISA_ACTIVE_color3_foto2_t0qme2.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (44, 12, 'PP', 'SKU-CAMISA-ACTIVE-7-COLOR3-PP', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (45, 12, 'P', 'SKU-CAMISA-ACTIVE-7-COLOR3-P', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (46, 12, 'M', 'SKU-CAMISA-ACTIVE-7-COLOR3-M', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (47, 12, 'G', 'SKU-CAMISA-ACTIVE-7-COLOR3-G', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (13, 7, 'Variação 4', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756128/CAMISA_ACTIVE_color4_foto1_pgut24.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_ACTIVE_color4_foto2_ekimhw.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (48, 13, 'P', 'SKU-CAMISA-ACTIVE-7-COLOR4-P', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (49, 13, 'M', 'SKU-CAMISA-ACTIVE-7-COLOR4-M', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (50, 13, 'G', 'SKU-CAMISA-ACTIVE-7-COLOR4-G', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (51, 13, 'GG', 'SKU-CAMISA-ACTIVE-7-COLOR4-GG', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (52, 13, 'XG', 'SKU-CAMISA-ACTIVE-7-COLOR4-XG', 2);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (8, 'CAMISA AMPLA REGULAR FIT EFEITO SUEDE', 'camisa-ampla-regular-fit-efeito-suede-9', 'Descubra a elegância do(a) Camisa Ampla Regular Fit Efeito Suede. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (8, 'Algodão', 95), (8, 'Elastano', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (14, 8, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756130/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_capa_wwfqbj.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_3_azb2ue.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (14, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756129/CAMISA_AMPLA_REGULAR_FIT_EFEITO_SUEDE_3_azb2ue.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (53, 14, 'P', 'SKU-CAMISA-AMPLA-REGULAR-FIT-EFEITO-SUEDE-9-DEFAULT-P', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (54, 14, 'M', 'SKU-CAMISA-AMPLA-REGULAR-FIT-EFEITO-SUEDE-9-DEFAULT-M', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (55, 14, 'G', 'SKU-CAMISA-AMPLA-REGULAR-FIT-EFEITO-SUEDE-9-DEFAULT-G', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (56, 14, 'GG', 'SKU-CAMISA-AMPLA-REGULAR-FIT-EFEITO-SUEDE-9-DEFAULT-GG', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (9, 'CAMISA BOX FIT EM JEANS LEVE', 'camisa-box-fit-em-jeans-leve-10', 'Descubra a elegância do(a) Camisa Box Fit Em Jeans Leve. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 249.90, 'SHIRTS', 'MEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (9, 'Algodão', 95), (9, 'Elastano', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (15, 9, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756131/CAMISA_BOX_FIT_EM_JEANS_LEVE_capa_kpbq9q.jpg', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756131/CAMISA_BOX_FIT_EM_JEANS_LEVE_hover_jpguxd.jpg');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (15, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784756130/CAMISA_BOX_FIT_EM_JEANS_LEVE_3_y1nwyu.jpg');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (57, 15, 'PP', 'SKU-CAMISA-BOX-FIT-EM-JEANS-LEVE-10-DEFAULT-PP', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (58, 15, 'P', 'SKU-CAMISA-BOX-FIT-EM-JEANS-LEVE-10-DEFAULT-P', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (59, 15, 'M', 'SKU-CAMISA-BOX-FIT-EM-JEANS-LEVE-10-DEFAULT-M', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (60, 15, 'G', 'SKU-CAMISA-BOX-FIT-EM-JEANS-LEVE-10-DEFAULT-G', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (61, 15, 'GG', 'SKU-CAMISA-BOX-FIT-EM-JEANS-LEVE-10-DEFAULT-GG', 7);

-- Jaquetas e Casacos femininos

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (10, 'CASACO DE MOLETOM COM ZÍPER', 'casaco-de-moletom-com-ziper-10', 'Descubra a elegância do(a) Casaco De Moletom Com Zíper. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 349.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (10, 'Algodão', 80), (10, 'Poliéster', 20);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (16, 10, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_capa_wu8uyf.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_hover_u3jqgp.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (16, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_3_vnns9t.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (62, 16, 'PP', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-DEFAULT-PP', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (63, 16, 'P', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-DEFAULT-P', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (64, 16, 'M', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-DEFAULT-M', 18);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (65, 16, 'G', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-DEFAULT-G', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (66, 16, 'GG', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-DEFAULT-GG', 0);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (17, 10, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto1_ig1vb4.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto2_ttmbit.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (17, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color2_foto3_m3cumt.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (67, 17, 'P', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR2-P', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (68, 17, 'M', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR2-M', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (69, 17, 'G', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR2-G', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (70, 17, 'GG', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR2-GG', 5);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (18, 10, 'Variação 3', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831414/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto1_qmc0lw.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto2_zonogd.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (18, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/CASACO_DE_MOLETOM_COM_Z%C3%8DPER_color3_foto3_jsfo7o.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (71, 18, 'PP', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR3-PP', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (72, 18, 'P', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR3-P', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (73, 18, 'M', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR3-M', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (74, 18, 'G', 'SKU-CASACO-DE-MOLETOM-COM-ZIPER-10-COLOR3-G', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (11, 'JAQUETA BOMBER COM PUNHOS EM CONTRASTE', 'jaqueta-bomber-com-punhos-em-contraste-11', 'Descubra a elegância do(a) Jaqueta Bomber Com Punhos Em Contraste. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 429.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (11, 'Poliéster', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (19, 11, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_capa_dvm1bd.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_hover_rbt1ka.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (19, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_3_y3yiwk.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (19, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_4_hjpzm7.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (75, 19, 'PP', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-DEFAULT-PP', 6);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (76, 19, 'P', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-DEFAULT-P', 11);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (77, 19, 'M', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-DEFAULT-M', 20);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (78, 19, 'G', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-DEFAULT-G', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (79, 19, 'GG', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-DEFAULT-GG', 3);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (20, 11, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831415/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_color2_foto1_ulbw7c.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831416/JAQUETA_BOMBER_COM_PUNHOS_EM_CONTRASTE_color2_foto2_g1omp0.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (80, 20, 'P', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-COLOR2-P', 0);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (81, 20, 'M', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-COLOR2-M', 9);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (82, 20, 'G', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-COLOR2-G', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (83, 20, 'GG', 'SKU-JAQUETA-BOMBER-COM-PUNHOS-EM-CONTRASTE-11-COLOR2-GG', 7);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (12, 'JAQUETA CURTA ACOLCHOADA', 'jaqueta-curta-acolchoada-12', 'Descubra a elegância do(a) Jaqueta Curta Acolchoada. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 459.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (12, 'Poliamida', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (21, 12, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_capa_ca8wvs.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_hover_zgb6cx.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (21, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_3_aculcg.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (84, 21, 'PP', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-DEFAULT-PP', 5);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (85, 21, 'P', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-DEFAULT-P', 13);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (86, 21, 'M', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-DEFAULT-M', 17);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (87, 21, 'G', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-DEFAULT-G', 10);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (22, 12, 'Variação 2', '#F5F5DC', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831417/JAQUETA_CURTA_ACOLCHOADA_color2_foto1_re1nbl.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_color2_foto2_yivjlz.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (22, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_CURTA_ACOLCHOADA_color2_foto3_jtkth8.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (88, 22, 'P', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-COLOR2-P', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (89, 22, 'M', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-COLOR2-M', 15);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (90, 22, 'G', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-COLOR2-G', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (91, 22, 'GG', 'SKU-JAQUETA-CURTA-ACOLCHOADA-12-COLOR2-GG', 0);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (13, 'JAQUETA DE POLIURETANO COM BOLSOS', 'jaqueta-de-poliuretano-com-bolsos-13', 'Descubra a elegância do(a) Jaqueta De Poliuretano Com Bolsos. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 499.9, 'JACKETS', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (13, 'Poliuretano', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (23, 13, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_capa_zop5d6.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_hover_siupa0.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (23, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/JAQUETA_DE_POLIURETANO_COM_BOLSOS_3_ilifv8.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (92, 23, 'PP', 'SKU-JAQUETA-DE-POLIURETANO-COM-BOLSOS-13-DEFAULT-PP', 3);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (93, 23, 'P', 'SKU-JAQUETA-DE-POLIURETANO-COM-BOLSOS-13-DEFAULT-P', 10);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (94, 23, 'M', 'SKU-JAQUETA-DE-POLIURETANO-COM-BOLSOS-13-DEFAULT-M', 16);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (95, 23, 'G', 'SKU-JAQUETA-DE-POLIURETANO-COM-BOLSOS-13-DEFAULT-G', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (96, 23, 'GG', 'SKU-JAQUETA-DE-POLIURETANO-COM-BOLSOS-13-DEFAULT-GG', 6);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (14, 'TRENCH CURTO CRUZADO', 'trench-curto-cruzado-14', 'Descubra a elegância do(a) Trench Curto Cruzado. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 549.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (14, 'Algodão', 65), (14, 'Poliéster', 35);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (24, 14, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_capa_eauecu.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831420/TRENCH_CURTO_CRUZADO_hover_cafs0v.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (24, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/TRENCH_CURTO_CRUZADO_3_wdvj2c.webp');
INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (24, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831418/TRENCH_CURTO_CRUZADO_4_cuvudf.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (97, 24, 'PP', 'SKU-TRENCH-CURTO-CRUZADO-14-DEFAULT-PP', 4);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (98, 24, 'P', 'SKU-TRENCH-CURTO-CRUZADO-14-DEFAULT-P', 12);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (99, 24, 'M', 'SKU-TRENCH-CURTO-CRUZADO-14-DEFAULT-M', 19);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (100, 24, 'G', 'SKU-TRENCH-CURTO-CRUZADO-14-DEFAULT-G', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (101, 24, 'GG', 'SKU-TRENCH-CURTO-CRUZADO-14-DEFAULT-GG', 2);

INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES
    (15, 'TRENCH CURTO CRUZADO DE COURO', 'trench-curto-cruzado-de-couro-15', 'Descubra a elegância do(a) Trench Curto Cruzado De Couro. Confeccionado com tecidos premium, oferecendo um caimento impecável. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas.', 699.9, 'COATS_AND_TRENCHES', 'WOMEN', null, false, true);

INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES (15, 'Couro', 100);

INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES
    (25, 15, 'Principal', '#000000', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_DE_COURO_capa_ob8lw1.webp', 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831420/TRENCH_CURTO_CRUZADO_DE_COURO_hover_bokyrw.webp');

INSERT INTO product_gallery_images (product_color_id, image_url) VALUES (25, 'https://res.cloudinary.com/apgaq55g/image/upload/v1784831419/TRENCH_CURTO_CRUZADO_DE_COURO_3_odsvhe.webp');

INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (102, 25, 'PP', 'SKU-TRENCH-CURTO-CRUZADO-DE-COURO-15-DEFAULT-PP', 2);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (103, 25, 'P', 'SKU-TRENCH-CURTO-CRUZADO-DE-COURO-15-DEFAULT-P', 8);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (104, 25, 'M', 'SKU-TRENCH-CURTO-CRUZADO-DE-COURO-15-DEFAULT-M', 14);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (105, 25, 'G', 'SKU-TRENCH-CURTO-CRUZADO-DE-COURO-15-DEFAULT-G', 7);
INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES (106, 25, 'GG', 'SKU-TRENCH-CURTO-CRUZADO-DE-COURO-15-DEFAULT-GG', 0);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));
SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));
