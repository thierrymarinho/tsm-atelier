# TSM Atelier — API

API REST de e-commerce de moda, construída como projeto de estudo para aprender arquitetura de
backend na prática: autenticação por cookie, controle de estoque sob concorrência, pagamento com
Stripe e um painel administrativo com trilha de auditoria.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1)
![Redis](https://img.shields.io/badge/Redis-7-DC382D)

![Testes](https://img.shields.io/badge/testes-347-success)

![CI](https://github.com/thierrymarinho/tsm-atelier/actions/workflows/ci.yml/badge.svg)

**▶ [Ver rodando](https://tsm-atelier-front.vercel.app/)** — front na Vercel, esta API no Render,
Postgres no Neon, Redis no Upstash. **Tudo em plano gratuito**, o que significa que o backend
hiberna após 15 minutos sem tráfego: se a primeira tela demorar, é a API acordando (~1 min).

O **painel administrativo** pode ser aberto com uma conta somente-leitura:

```
demo@tsm-atelier.com  ·  demo1234
```

Ela enxerga dashboard, produtos, coleções e a trilha de auditoria, e não consegue escrever nada nem
abrir a tela de pedidos — ver a [decisão 8](#8-um-papel-de-leitura-para-o-painel-ser-visitável).

> **Projeto de portfólio, não um produto.** Ele roda de ponta a ponta e tem cobertura de teste real,
> mas existe para eu estudar decisões de backend — não para atender clientes. A seção
> [O que ficou de fora](#o-que-ficou-de-fora-e-por-quê) lista o que eu sei que falta, com o motivo.

---

## Sumário

- [Em produção](#em-produção)
- [O que a API faz](#o-que-a-api-faz)
- [Stack](#stack)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Arquitetura](#arquitetura)
- [Decisões técnicas](#decisões-técnicas)
- [Testes](#testes)
- [O que ficou de fora](#o-que-ficou-de-fora-e-por-quê)
- [Documentação](#documentação)

---

## Em produção

| Peça | Onde | Plano |
|---|---|---|
| Front (SPA) | Vercel | gratuito |
| Esta API | Render (Docker) | gratuito |
| Postgres | Neon | gratuito |
| Redis | Upstash | gratuito |
| Imagens | Cloudinary | gratuito |
| Pagamento · E-mail | Stripe (test mode) · Resend | gratuito |

O plano gratuito não é só uma nota de rodapé — ele **produziu decisões de arquitetura**, e as três
mais visíveis estão marcadas ao longo deste README:

- o Upstash cobra **por comando**, o que empurrou o access token para 15 minutos em vez de 1 (menos
  refresh = menos comandos) e criou a necessidade da denylist;
- a origem no Render é **publicamente alcançável**, então `X-Forwarded-For` é forjável e o lockout de
  conta teve que sair do par `email + IP` para só o e-mail;
- o Redis é **um só**, dividido entre cache e estado de sessão, com `maxmemory` e política de despejo
  escolhidas sabendo que sob pressão um refresh token pode ser descartado.

### O rewrite, verificado em produção

A [decisão 1](#1-mesma-origem-por-rewrite--e-nenhum-cors) diz que o navegador nunca fala com o
backend e que não existe CORS. Os headers de uma requisição real ao ambiente publicado mostram isso:

```console
$ curl -sD - -o /dev/null https://tsm-atelier-front.vercel.app/api/v1/catalog/collections

server: Vercel                          ← quem responde ao navegador
x-render-origin-server: Render          ← quem realmente processou
set-cookie: __Host-XSRF-TOKEN=...; Path=/; Secure; SameSite=Strict
x-content-type-options: nosniff
```

Repare no que **não** está ali: nenhum header `access-control-*`. Não há CORS porque, do ponto de
vista do navegador, não existe segunda origem — o `__Host-` no cookie só é aceito sob HTTPS com
`Path=/` e sem `Domain`, que é exatamente o arranjo que o rewrite garante.

---

## O que a API faz

São 50 endpoints, divididos em quatro áreas:

| Área | O que cobre |
|---|---|
| **Autenticação** | Cadastro com verificação de e-mail, login, refresh com rotação, logout com revogação |
| **Catálogo** (público) | Busca paginada com filtros, detalhe por id ou slug, coleções, facetas (materiais, cuidados) |
| **Loja** (sessão) | Carrinho, checkout com reserva de estoque, pagamento Stripe, endereços, histórico de pedidos |
| **Admin** (`ROLE_ADMIN`) | CRUD de produtos e SKUs, estoque, coleções, gestão de pedidos, dashboard, trilha de auditoria |

O domínio é uma loja de roupas de verdade: produto tem **SKU** por combinação de cor e tamanho, com
estoque próprio, composição de tecido e instruções de cuidado — não é um CRUD de "produto com
quantidade".

---

## Stack

| Camada | Escolha | Por quê |
|---|---|---|
| Linguagem | Java 25 | DTOs como `record` (50 arquivos), toolchain fixado no Gradle |
| Framework | Spring Boot 4.1 · Spring Security 7.1 | Versões novas o bastante para não haver tutorial pronto — parte do exercício |
| Persistência | PostgreSQL 17 · Spring Data JPA · Flyway | Schema versionado em 11 migrations, `ddl-auto: validate` — o Hibernate nunca altera a tabela |
| Cache e estado | Redis 7 | Cache do catálogo, refresh tokens, denylist e rate limit |
| Build | Gradle (Kotlin DSL) · Spotless · JaCoCo | Formatação verificada na CI, relatório de cobertura no `build` |
| Integrações | Stripe (pagamento) · Cloudinary (imagens) · Resend (e-mail) | Cada uma atrás de uma porta do domínio, trocável sem tocar em regra de negócio |
| Deploy | Docker multi-stage · Render · Neon · Upstash | Imagem sem root e JVM ciente do limite do contêiner; o serviço está descrito em `render.yaml` |
| CI | GitHub Actions com Postgres e Redis reais | Os testes de integração sobem contra serviço, não contra mock |

---

## Como rodar

**Pré-requisitos:** Docker e JDK 25. O Gradle vem pelo wrapper.

```bash
# 1. banco e cache
docker compose up -d

# 2. as variáveis sem valor padrão (sem elas a aplicação não sobe)
export DB_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 48)
export STRIPE_API_KEY=sk_test_x STRIPE_WEBHOOK_SECRET=whsec_x
export ADMIN_PASSWORD_HASH='$2a$10$U7EJsOg3No2BaPaXFnKWpOEFWXgOHhPQ5UBdS7SWgW8xWBWNhTB1C'

# 3. sobe
./gradlew bootRun
```

A API responde em `http://localhost:8080/api/v1`. O Flyway cria o schema e popula o catálogo inicial
na primeira execução.

O `ADMIN_PASSWORD_HASH` acima corresponde à senha `senha123` e serve **só para desenvolvimento** — a
migration `V10` recusa subir se o valor não for um hash BCrypt, justamente para que ninguém publique
com senha em texto puro. Para gerar o seu:

```bash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'SUA_SENHA', bcrypt.gensalt(10, prefix=b'2a')).decode())"
```

Rodar a suíte:

```bash

./gradlew test          # 347 testes; o relatório do JaCoCo sai em build/jacocoHtml
=======
./gradlew spotlessCheck # formatação
```

---

## Variáveis de ambiente

Cinco variáveis **não têm valor padrão de propósito**: sem elas a aplicação não sobe. É uma decisão
— um segredo com fallback silencioso é pior que uma falha no boot, porque o deploy fica verde com a
segurança desligada.

| Variável | Obrigatória | Para quê |
|---|:---:|---|
| `DB_PASSWORD` | ✅ | Senha do Postgres |
| `JWT_SECRET` | ✅ | Assinatura dos tokens (mínimo 32 bytes, validado no boot) |
| `ADMIN_PASSWORD_HASH` | ✅ | Hash BCrypt do admin inicial, injetado na migration |
| `STRIPE_API_KEY` · `STRIPE_WEBHOOK_SECRET` | ✅ | Pagamento e verificação de assinatura do webhook |
| `SPRING_DATASOURCE_URL` · `DB_USER` | | Padrão: Postgres local |
| `REDIS_HOST` · `REDIS_PORT` · `REDIS_PASSWORD` · `REDIS_SSL_ENABLED` | | Padrão: Redis local sem TLS |
| `CLOUDINARY_*` | | Upload de imagem; vazio desliga a funcionalidade |
| `RESEND_API_KEY` · `RESEND_FROM_EMAIL` | | Envio de e-mail; vazio faz o envio falhar e logar, sem quebrar o fluxo |
| `APP_BASE_URL` | | URL **do front**, usada para montar o link de verificação |

O [`render.yaml`](render.yaml) descreve o serviço publicado — plano, health check, e cada uma das 21
variáveis com um comentário do porquê. Ele vale como **documentação, não como Blueprint aplicado**: o
serviço foi criado pelo painel do Render, então o arquivo não é lido pela plataforma. O aviso está no
topo dele, porque confundir os dois já custou um deploy — `generateValue: true` no `JWT_SECRET`
descreve o que o Render *faria* sob Blueprint, e sem Blueprint ele não gera nada.

---

## Arquitetura

O código é organizado **por domínio**, não por camada técnica — `product/`, `order/`, `cart/`,
`auth/` são pacotes de primeiro nível, cada um com seu controller, serviço, repositório e DTOs:

```
src/main/java/com/tm/tsm_atelier/
├── domain/          product · collection · cart · order · user · auth · admin
├── infrastructure/  email · payment · storage · scheduler   (adaptadores externos)
├── security/        cadeia de filtros, JWT, CSRF, rate limit, denylist
├── config/          Redis, Cloudinary, Resend, Async, JPA
└── common/          tratamento de erro, logging, utilitários web
```

As integrações externas ficam atrás de uma **porta** (`EmailPort`, `PaymentGatewayPort`, `StoragePort`)
declarada no domínio e implementada em `infrastructure/`. O serviço de pedido não sabe que existe
Stripe; o de autenticação não sabe que existe Resend. É o que torna os testes de fluxo possíveis sem
rede.

### A cadeia de segurança

```
navegador → rewrite da Vercel → Render → Tomcat
                                            │
   RequestIdFilter → ForwardedHeaderFilter  │  fora do Spring Security
                                            │
   CsrfFilter ─────────────── 403 se faltar o header
   JwtAuthenticationFilter ── só POPULA o contexto, nunca rejeita
   CsrfCookieFilter ───────── emite o cookie CSRF
   AuthorizationFilter ────── aqui nascem o 401 e o 403
                                            │
                                       Controller
```

A separação que importa: **o filtro de JWT não decide nada.** Ele responde "quem é você"; quem
responde "você pode" é o `AuthorizationFilter`, no fim da cadeia. Manter as duas responsabilidades
separadas é o que faz `401` (não sei quem você é) e `403` (sei, e você não pode) significarem coisas
diferentes de verdade.

---

## Decisões técnicas

Esta é a parte do projeto que eu acho que vale a leitura. Cada decisão abaixo saiu de uma restrição
concreta, e cada uma tem um custo que eu escolhi pagar.

### 1. Mesma origem por rewrite — e nenhum CORS

O front está na Vercel e a API no Render: plataformas diferentes, origens diferentes. E origem
diferente quebra autenticação por cookie — o Safari bloqueia cookie de terceiro por padrão, o Firefox
particiona. Como o projeto precisa abrir no celular de quem for ver, isso eliminou o arranjo
cross-site antes de qualquer discussão sobre esquema de autenticação.

A solução foi um **rewrite na Vercel**: front e API respondem pelo mesmo hostname, e o navegador
nunca vê o backend. A consequência é que **não existe configuração de CORS neste repositório** — não
por esquecimento, mas porque não há origem externa a autorizar.

### 2. Cookie `HttpOnly`, não token no `localStorage`

Uma troca consciente: cookie `HttpOnly` expõe a CSRF e protege de XSS; `localStorage` é exatamente o
inverso. Escolhi pagar o CSRF porque é uma dívida com solução conhecida e **testável** — existem
testes fixando os atributos do cookie e o comportamento do filtro. XSS lendo `localStorage` não tem
mitigação equivalente: se vazou, vazou.

O CSRF é resolvido por **double-submit cookie**. A segurança está numa assimetria: um site hostil
consegue fazer o navegador enviar o cookie — é automático, é justamente o problema — mas não consegue
**ler** o valor, porque a same-origin policy impede. Sem o valor, não há como montar o header.

Prefixos `__Host-` e `__Secure-` nos cookies, `SameSite=Strict`, e o refresh token com `Path`
restrito a `/api/v1/auth` — ele não é enviado em nenhuma outra requisição.

### 3. Rotação de refresh token com detecção de reuso

Access token de 15 minutos, refresh de 7 dias. Cada refresh **consome** o token e emite outro. Se um
token já consumido reaparece, só há uma explicação — existe uma cópia circulando — e todas as sessões
daquele usuário são revogadas.

Existe uma **janela de graça de 30 segundos** porque o cliente legítimo reaparece com o token antigo o
tempo todo: resposta perdida, timeout, duas abas renovando juntas. Sem ela, cada um desses casos
derrubava a sessão de um usuário inocente. O preço é que um replay dentro dos 30 segundos passa
despercebido — por isso ela é curta.

Revogar um JWT, em rigor, não é possível: ele é válido pela assinatura, não por consulta. O que a API
faz é **interceptar** — o logout grava no Redis o *hash* do token com TTL igual à validade restante, e
o filtro consulta essa lista. O hash, e não o token, para que um dump do Redis não devolva credencial
utilizável.

### 4. O cadastro não revela se o e-mail já existe

Devolver `409 Email is already in use` numa rota pública é responder "esta pessoa é cliente da loja"
para quem iterar uma lista de endereços. Hoje e-mail novo e e-mail existente recebem o **mesmo `201`
com a mesma mensagem**.

O que torna isso honesto é que o aviso não some, muda de canal: quem já tem conta recebe um e-mail
"você já tem uma conta", com link para o login. Sem essa metade, o vazamento teria virado um beco —
quem esqueceu que tinha conta veria "confira seu e-mail" e nada chegaria.

### 5. Estoque: bloqueio pessimista no SKU, reserva com prazo

O checkout **reserva** o estoque na hora e dá 30 minutos para o pagamento. A baixa usa
`PESSIMISTIC_WRITE` no SKU — duas pessoas comprando a última peça ao mesmo tempo serializam no banco,
e uma recebe erro em vez de as duas comprarem.

O pedido tem `@Version`, e um scheduler roda a cada minuto cancelando os expirados e **devolvendo o
estoque**. Ele trata `OptimisticLockingFailureException` como caso normal, não como falha: se o
pagamento chegou no mesmo instante em que o cancelamento ia rodar, quem perdeu a corrida
simplesmente desiste.

### 6. O item do pedido é um snapshot, não uma referência

`OrderItem` guarda nome, código do SKU, tamanho, cor, imagem e **os dois preços** — o pago e o de
tabela na data da compra. O `sku_id` é nullable de propósito.

O motivo: um pedido é um registro histórico. Se o item apontasse só para o produto, renomear a peça
reescreveria pedidos antigos, e excluir o produto apagaria o histórico de quem comprou. Guardar o
preço de tabela junto do pago é o que permite mostrar "você economizou X" meses depois, mesmo que a
promoção tenha acabado.

### 7. O cache degrada, não derruba

O `CacheErrorHandler` do Redis engole falhas de cache: se o Redis cair, o catálogo continua
respondendo, indo direto ao banco. A decisão vale também para a denylist, que **falha aberta** — um
token revogado sobrevive alguns minutos, o que é preferível a derrubar o site inteiro por causa de
uma dependência que existe apenas para revogar. O log sai em `warn`, porque o sistema está rodando
com uma garantia a menos e isso precisa aparecer em algum lugar.

### 8. Um papel de leitura, para o painel ser visitável

O painel é metade do trabalho e ficava invisível para quem só tem o link. A saída óbvia — publicar a
senha de um admin — entrega a exclusão do catálogo para qualquer visitante, então criei
`ROLE_ADMIN_VIEWER`: aceito **apenas em GET**, e apenas em dashboard, produtos, coleções e auditoria.

O que decidiu o recorte não foi o que cada tela mostra, foi o que cada rota **aceita como pergunta**.
Pedidos ficaram de fora, e não por causa do `customerEmail` na resposta — mascarar isso é fácil. É
que `GET /admin/orders` aceita `searchTerm`, e o filtro casa por substring no e-mail e no nome:

```java
// OrderSpecification:27-29
matches.add(cb.like(cb.lower(root.get("user").get("email")), pattern));
matches.add(cb.like(cb.lower(root.get("user").get("firstName")), pattern));
```

Com isso, mascarar a saída não fecha nada — quem consulta alonga o termo um caractere por vez e
observa quais consultas devolvem resultado, reconstruindo o endereço sem nunca vê-lo. **É a mesma
enumeração da decisão 4, por outra porta**: lá o cadastro respondia "esta pessoa é cliente", aqui a
busca responde o mesmo e ainda soletra. A defesa tem que estar na consulta, não na renderização.

Estoque e auditoria entraram porque foram verificados, não presumidos: `stockQuantity` já sai no
catálogo público, e `AuditedEntity` não tem `USER` — o histórico registra preço, status e estoque,
nunca dado de cliente.

O papel depende do verbo HTTP, e "GET é leitura" é convenção, não garantia. Por isso o
`AdminViewerAuthorizationTest` fixa os dois lados — o que o viewer alcança e o que não alcança — com
CSRF válido nas escritas, senão o `CsrfFilter` responderia `403` e o teste ficaria verde mesmo se a
exigência de papel sumisse.

---

## Testes

**347 testes.** O projeto tem mais linhas de teste (~8.000) do que de produção (~7.550), e isso não é

acidente: quase todo bug corrigido virou um teste que falha sem a correção.

| Tipo | O que cobre |
|---|---|
| Unitário | Regras de serviço com dependências mockadas |
| `@WebMvcTest` | Contrato de rota: status, formato de erro, validação de payload |
| `@SpringBootTest` | Fluxos completos contra Postgres e Redis reais — checkout, auditoria, roteamento por slug |
| Servidor real | Os testes de segurança que o MockMvc não consegue provar |

---

## O que ficou de fora (e por quê)

Nenhum destes é "não deu tempo". Todos têm um motivo e um caminho de saída.

1. **O IP não é confiável.** A origem no Render é publicamente alcançável, então `X-Forwarded-For` é
   forjável e nenhum limite por IP vale como defesa. Contornado movendo o lockout de conta para o
   e-mail — antes era `email + IP`, o que não trancava conta nenhuma, trancava o par. Resolver de
   verdade exige um segredo compartilhado entre o proxy e o backend.

2. **Access token sobrevive à revogação em massa.** Quando a detecção de reuso dispara, não tenho os
   access tokens para revogar — eles nunca foram armazenados, que é o que *stateless* significa.
   Valem até 15 minutos. Fecharia com um `notBefore` por usuário, lido no mesmo `MGET` da denylist
   para não pagar uma ida a mais ao Redis por requisição.

3. **A denylist falha aberta.** Redis indisponível significa token revogado aceito até expirar.
   Escolhido conscientemente — ver a decisão 7.

4. **`HttpOnly` não é prova de posse.** Ele impede *ler*, não impede *apresentar*. Cookie obtido por
   outro caminho — extensão de navegador, máquina compartilhada — é indistinguível do legítimo.

5. **`SameSite=Strict` impede verificar sessão em SSR.** Quem chega por link externo faz a primeira
   navegação sem cookie. É transparente para este front, que autentica no cliente; se ele passar a
   depender de SSR autenticado, o valor tem que voltar para `Lax`.

6. **Cache e credencial dividem o mesmo Redis.** Com `allkeys-lru`, sob pressão de memória o despejo
   pode atingir um refresh token, não só uma página de catálogo. As alternativas eram piores:
   `noeviction` faria o `SET` de um login falhar, e sem teto um OOM perderia tudo de uma vez.
   Separar exige uma segunda instância — `maxmemory` é por processo, não por database.

---

## Documentação

| Arquivo | Para quem |
|---|---|
| [`API_REFERENCE.md`](API_REFERENCE.md) | Referência completa das rotas — autenticação, formato de erro, paginação, enums, todos os endpoints com exemplo |
| [`ADMIN_FRONTEND_SPEC.md`](ADMIN_FRONTEND_SPEC.md) | Especificação do painel administrativo para quem for implementar o front |
| [`tsm-atelier-postman_collection.json`](tsm-atelier-postman_collection.json) | Coleção do Postman com os fluxos prontos |
| [`render.yaml`](render.yaml) · [`Dockerfile`](Dockerfile) | Infraestrutura, com o raciocínio de cada escolha em comentário — o `render.yaml` é descritivo, não aplicado |
