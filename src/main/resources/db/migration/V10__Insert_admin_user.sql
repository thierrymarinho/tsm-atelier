-- O primeiro admin.
--
-- Sem ele o painel era inalcançável: POST /api/v1/auth/register cria sempre
-- CUSTOMER, não existe rota para promover ninguém, e todo /api/v1/admin/**
-- exige ROLE_ADMIN. A única saída era um UPDATE manual no banco de produção.
--
-- A senha não está aqui. O que entra é o hash resolvido de
-- `spring.flyway.placeholders.admin_password_hash`, que em produção vem da
-- variável de ambiente ADMIN_PASSWORD_HASH e em desenvolvimento cai no padrão
-- do application.yaml — `senha123`, dita em texto claro lá justamente por não
-- valer nada fora da máquina de quem desenvolve.
--
-- Isto funciona porque o Flyway calcula o checksum sobre o arquivo cru, antes
-- de substituir o placeholder: produção e desenvolvimento gravam senhas
-- diferentes e continuam validando contra o mesmo histórico.
--
-- Para gerar um hash novo (BCrypt força 10, o padrão de
-- `new BCryptPasswordEncoder()` em SecurityConfig):
--
--   python3 -c "import bcrypt;print(bcrypt.hashpw(b'SUA_SENHA', bcrypt.gensalt(10, prefix=b'2a')).decode())"
--
-- email_verified vem TRUE de propósito: a verificação depende de um link
-- enviado por e-mail, e este endereço não existe em caixa nenhuma. Deixá-lo
-- falso tornaria a conta tão inacessível quanto a ausência dela.
--
-- ON CONFLICT DO NOTHING para a migration ser aplicável a uma base que já
-- tenha esse e-mail cadastrado — reaproveitar a linha existente é melhor do
-- que falhar o boot inteiro por causa de um UNIQUE.
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
