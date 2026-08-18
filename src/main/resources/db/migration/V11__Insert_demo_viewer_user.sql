-- Conta de demonstracao do painel, somente leitura.
--
-- A senha esta em texto conhecido de proposito, e por isso nao usa placeholder
-- como o admin do V10: ela existe para ser publicada no README, de modo que
-- quem chega ao projeto consiga abrir o painel sem pedir credencial a ninguem.
-- Publicar a senha de um ADMIN seria entregar a exclusao do catalogo; o que
-- torna isto seguro nao e a senha, e o papel.
--
-- ROLE_ADMIN_VIEWER alcanca apenas os GET de dashboard, produtos, colecoes e
-- auditoria (SecurityConstants.ADMIN_VIEWER_ROUTES). Nao alcanca pedidos, que
-- e onde vivem nome, e-mail e endereco de cliente.
--
-- Se este repositorio for reaproveitado para uma loja de verdade, esta migration
-- e a primeira coisa a remover.

-- senha: demo1234 — oito caracteres porque o cadastro exige min 8
-- (RegisterRequestDTO), e o formulario do painel espelha essa regra.
INSERT INTO users (first_name, last_name, email, password, email_verified, role)
VALUES (
    'Demonstracao',
    'TSM Atelier',
    'demo@tsm-atelier.com',
    '$2a$10$d/mZbFMFWukB/DTedraG1.p2sjickvNrWMPj5YBG6ahuAWpmQnIRy',
    TRUE,
    'ADMIN_VIEWER'
)
ON CONFLICT (email) DO NOTHING;
