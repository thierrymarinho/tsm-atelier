DO $$
BEGIN
    IF '${admin_password_hash}' NOT LIKE '$2%' THEN
        RAISE EXCEPTION 'ADMIN_PASSWORD_HASH nao foi definido, ou nao e um hash BCrypt. Gere um com: python3 -c "import bcrypt;print(bcrypt.hashpw(b''SUA_SENHA'', bcrypt.gensalt(10, prefix=b''2a'')).decode())"';
    END IF;
END $$;

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
