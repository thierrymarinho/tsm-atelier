import json
import os

FILE_PATH = 'tsm-atelier-postman_collection.json'

with open(FILE_PATH, 'r', encoding='utf-8') as f:
    collection = json.load(f)

# Find Orders folder
orders_folder = next((item for item in collection['item'] if item['name'] == 'Orders'), None)
if orders_folder:
    # Check if 'My Orders' exists
    if not any(req['name'] == '2. My Orders' for req in orders_folder['item']):
        orders_folder['item'].append({
            "name": "2. My Orders",
            "request": {
                "method": "GET",
                "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                "url": {
                    "raw": "{{base_url}}/api/v1/orders/my-orders",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "orders", "my-orders"]
                }
            }
        })
        
    if not any(req['name'] == '3. Order Details' for req in orders_folder['item']):
        orders_folder['item'].append({
            "name": "3. Order Details",
            "request": {
                "method": "GET",
                "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                "url": {
                    "raw": "{{base_url}}/api/v1/orders/1",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "orders", "1"]
                }
            }
        })

# Create Admin Orders folder if not exists
admin_orders_folder = next((item for item in collection['item'] if item['name'] == 'Admin Orders'), None)
if not admin_orders_folder:
    admin_orders_folder = {
        "name": "Admin Orders",
        "item": []
    }
    collection['item'].append(admin_orders_folder)

if not any(req['name'] == '1. List All Orders' for req in admin_orders_folder['item']):
    admin_orders_folder['item'].append({
        "name": "1. List All Orders",
        "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{admin_token}}"}],
            "url": {
                "raw": "{{base_url}}/api/v1/admin/orders",
                "host": ["{{base_url}}"],
                "path": ["api", "v1", "admin", "orders"]
            }
        }
    })

if not any(req['name'] == '2. Update Order Status' for req in admin_orders_folder['item']):
    admin_orders_folder['item'].append({
        "name": "2. Update Order Status",
        "request": {
            "method": "PATCH",
            "header": [{"key": "Authorization", "value": "Bearer {{admin_token}}"}],
            "url": {
                "raw": "{{base_url}}/api/v1/admin/orders/1/status?newStatus=SHIPPED",
                "host": ["{{base_url}}"],
                "path": ["api", "v1", "admin", "orders", "1", "status"],
                "query": [
                    {
                        "key": "newStatus",
                        "value": "SHIPPED"
                    }
                ]
            }
        }
    })

# Add Stripe Webhook
stripe_folder = next((item for item in collection['item'] if item['name'] == 'Webhooks'), None)
if not stripe_folder:
    stripe_folder = {
        "name": "Webhooks",
        "item": []
    }
    collection['item'].append(stripe_folder)

if not any(req['name'] == 'Stripe Webhook (Local Test via CLI)' for req in stripe_folder['item']):
    stripe_folder['item'].append({
        "name": "Stripe Webhook (Local Test via CLI)",
        "request": {
            "method": "POST",
            "header": [
                {"key": "Stripe-Signature", "value": "YOUR_STRIPE_SIGNATURE_HERE"}
            ],
            "body": {
                "mode": "raw",
                "raw": "{ \"id\": \"evt_123\", \"type\": \"payment_intent.succeeded\", \"data\": { \"object\": { \"id\": \"pi_123\" } } }"
            },
            "url": {
                "raw": "{{base_url}}/api/v1/webhooks/stripe",
                "host": ["{{base_url}}"],
                "path": ["api", "v1", "webhooks", "stripe"]
            }
        }
    })

with open(FILE_PATH, 'w', encoding='utf-8') as f:
    json.dump(collection, f, indent=4, ensure_ascii=False)

print("Postman collection updated successfully.")
