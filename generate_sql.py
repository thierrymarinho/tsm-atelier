import os
import re

directory = "/home/thierry/Development/fotos-atelier/feminino/vestidos"
files = [f for f in os.listdir(directory) if f.endswith(('.jpg', '.png', '.webp'))]
# Sort to ensure consistent order
files.sort()

products = {}

for f in files:
    # Match patterns like:
    # "VESTIDO DRAPEADO GOLA ALTA capa.webp"
    # "VESTIDO DRAPEADO GOLA ALTA hover.webp"
    # "VESTIDO DRAPEADO GOLA ALTA 3.webp"
    # "VESTIDO MIDI ACETINADO color2 foto1.jpg"
    match = re.match(r'^(.*?)(?:\s+(capa|hover|\d+|color\d+\s+foto\d+))?\.(webp|jpg|png|jpeg)$', f, re.IGNORECASE)
    if not match:
        print(f"Skipping file {f} - doesn't match expected pattern.")
        continue
    
    product_name = match.group(1).strip()
    modifier = match.group(2)
    
    if product_name not in products:
        products[product_name] = {
            'colors': {
                'default': {'cover': None, 'hover': None, 'gallery': []}
            }
        }
    
    if modifier:
        modifier = modifier.lower()
        if modifier == 'capa':
            products[product_name]['colors']['default']['cover'] = f
        elif modifier == 'hover':
            products[product_name]['colors']['default']['hover'] = f
        elif modifier.isdigit():
            products[product_name]['colors']['default']['gallery'].append(f)
        elif modifier.startswith('color'):
            # Example: color2 foto1
            color_match = re.match(r'(color\d+)\s+foto(\d+)', modifier)
            if color_match:
                color_key = color_match.group(1)
                if color_key not in products[product_name]['colors']:
                    products[product_name]['colors'][color_key] = {'cover': None, 'hover': None, 'gallery': []}
                
                foto_num = color_match.group(2)
                # Assign to cover, hover or gallery based on number
                if foto_num == '1':
                    products[product_name]['colors'][color_key]['cover'] = f
                elif foto_num == '2':
                    products[product_name]['colors'][color_key]['hover'] = f
                else:
                    products[product_name]['colors'][color_key]['gallery'].append(f)
    else:
        # No modifier, use as cover
        if not products[product_name]['colors']['default']['cover']:
            products[product_name]['colors']['default']['cover'] = f
        else:
            products[product_name]['colors']['default']['gallery'].append(f)

# Let's generate SQL
sql = "-- Migration gerada automaticamente a partir das imagens\n\n"
product_id = 1
color_id = 1
sku_id = 1

for name, data in products.items():
    slug = name.lower().replace(' ', '-').replace('ê', 'e').replace('ç', 'c').replace('ã', 'a') + f"-{product_id}"
    desc = f"Descubra a elegância do {name.title()}. Confeccionado com tecidos premium, este vestido oferece um caimento impecável que valoriza a silhueta. Perfeito para ocasiões onde você deseja se destacar com sofisticação e conforto, combinando estilo atemporal com tendências modernas."
    
    sql += f"INSERT INTO products (id, name, slug, description, price, category, target_audience, collection_id, is_featured, active) VALUES \n"
    sql += f"({product_id}, '{name}', '{slug}', '{desc}', 299.90, 'DRESSES', 'WOMEN', 1, true, true);\n\n"
    
    sql += f"INSERT INTO product_fabric_compositions (product_id, material, percentage) VALUES "
    sql += f"({product_id}, 'Viscose', 90), ({product_id}, 'Elastano', 10);\n\n"
    
    for color_key, cdata in data['colors'].items():
        color_name = "Principal" if color_key == 'default' else f"Variação {color_key.replace('color', '')}"
        color_hex = "#000000" if color_key == 'default' else "#F5F5DC"
        
        # O usuário pediu para deixar as URLs em branco '' para colar manualmente depois
        cover = ''
        hover = ''
        
        sql += f"INSERT INTO product_colors (id, product_id, color_name, color_hex, cover_image_url, hover_image_url) VALUES \n"
        sql += f"({color_id}, {product_id}, '{color_name}', '{color_hex}', '{cover}', '{hover}');\n\n"
        
        # Gallery Images
        for g in cdata['gallery']:
            sql += f"INSERT INTO product_gallery_images (product_color_id, image_url) VALUES ({color_id}, '');\n"
            
        sql += "\n"
        
        # SKUs
        for size in ['P', 'M', 'G']:
            sku_code = f"SKU-{slug.upper()}-{color_key.upper()}-{size}"
            sql += f"INSERT INTO product_skus (id, product_color_id, size, sku_code, stock_quantity) VALUES "
            sql += f"({sku_id}, {color_id}, '{size}', '{sku_code}', 15);\n"
            sku_id += 1
            
        sql += "\n"
        color_id += 1
        
    product_id += 1

# Reset sequence
sql += f"\nSELECT setval('products_id_seq', (SELECT MAX(id) FROM products));\n"
sql += f"SELECT setval('product_colors_id_seq', (SELECT MAX(id) FROM product_colors));\n"
sql += f"SELECT setval('product_skus_id_seq', (SELECT MAX(id) FROM product_skus));\n"

output_path = "/home/thierry/Development/repositories/tsm-atelier/src/main/resources/db/migration/V5__Insert_products.sql"
with open(output_path, "w") as f:
    f.write(sql)

print(f"SQL Generated successfully at {output_path}!")
