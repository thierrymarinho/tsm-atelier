INSERT INTO users (first_name, last_name, email, password, email_verified, role)
VALUES (
    'Admin',
    'TSM Atelier',
    'admin@tsm-atelier.com',
    '${admin_password_hash}',
    TRUE,
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;
