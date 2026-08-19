# TSM Atelier — Referência da API para implementação do front-end

Contrato completo da API: todos os endpoints, DTOs de entrada e saída, formatos de erro e regras de autorização. Todos os exemplos de JSON foram capturados da aplicação rodando, não escritos de memória.

Base: `/api/v1`

---

## 1. Regras que valem para a API inteira

Leia esta seção antes de qualquer endpoint. Ela evita a maior parte dos erros de integração.

### 1.1 Autenticação é por cookie, nunca por header

Não existe suporte a `Authorization: Bearer`. Foi removido de propósito. A sessão viaja em dois cookies `HttpOnly` que o browser envia sozinho:

| Cookie | Validade | Path | Atributos |
|---|---|---|---|
| `__Host-access_token` | 15 min | `/` | `HttpOnly` `Secure` `SameSite=Strict` |
| `__Secure-refresh_token` | 7 dias | `/api/v1/auth` | `HttpOnly` `Secure` `SameSite=Strict` |
| `__Host-XSRF-TOKEN` | sessão | `/` | `Secure` `SameSite=Strict` — **legível por JS** |

Os prefixos `__Host-` e `__Secure-` fazem parte do nome do cookie, e não são decorativos: o browser recusa gravar o cookie se os atributos que o prefixo exige não estiverem presentes.

O front **não consegue e não precisa** ler os dois primeiros. Como front e API respondem pela mesma origem (via rewrite do Next), os cookies vão automaticamente — `credentials: 'include'` é desnecessário. Chamadas devem usar **caminho relativo**; URL absoluta para o backend quebra a origem única e nada funciona.

### 1.2 Toda escrita exige o header CSRF

`POST`, `PUT`, `PATCH` e `DELETE` exigem o header `X-XSRF-TOKEN` com o valor do cookie `__Host-XSRF-TOKEN`. `GET` nunca exige.

```ts
const csrf = document.cookie.split('; ').find(c => c.startsWith('__Host-XSRF-TOKEN='))?.split('=')[1]
```

O axios faz isso sozinho com `xsrfCookieName: '__Host-XSRF-TOKEN'` (o nome do **header** continua `X-XSRF-TOKEN`).

**Exceções** (não exigem CSRF): `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/refresh`, `/webhooks/stripe`. O `/auth/logout` **exige**.

O cookie `__Host-XSRF-TOKEN` é emitido em qualquer resposta que passe pela cadeia de segurança — um `GET /api/v1/catalog/products` na inicialização já basta.

> O fluxo completo de autenticação — inicialização, interceptor, estratégia de refresh e tratamento de erro — está na [§4.1](#41-implementando-a-autenticação-no-front).

### 1.3 Formato de erro: `ProblemDetail` (RFC 7807)

**Todo** erro tem o mesmo envelope:

```json
{
  "status": 404,
  "title": "Resource not found",
  "detail": "Product not found with identifier: 999999",
  "instance": "/api/v1/catalog/products/999999"
}
```

Duas variações acrescentam um campo:

**422 — validação de campos** traz `fields`, um mapa `campo → mensagem`:

```json
{
  "status": 422,
  "title": "Validation error",
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/auth/register",
  "fields": {
    "firstName": "First name must be between 2 and 50 characters",
    "email": "Invalid email format",
    "password": "Password must be between 8 and 72 characters"
  }
}
```

Em coleções, a chave usa índice: `"fields": { "items[0].quantity": "Quantity is required" }`.

**409 de estoque** diz qual item falhou e por quê:

```json
{
  "status": 409,
  "title": "Out of stock",
  "detail": "Out of stock for SKU: TSM-000014. Available: 0",
  "instance": "/api/v1/orders/checkout",
  "availableQuantity": 0,
  "skuId": 14,
  "reason": "INSUFFICIENT_STOCK"
}
```

| `reason` | Quando acontece | `availableQuantity` | Extra |
|---|---|---|---|
| `INSUFFICIENT_STOCK` | o estoque acabou ou não cobre a quantidade pedida | o que ainda resta | — |
| `PRODUCT_UNAVAILABLE` | o produto foi desativado | sempre `0` | — |
| `MAX_UNITS_PER_ITEM` | passou do teto por pedido | o próprio teto | `maxUnitsPerItem` |

> **Case o item pelo `skuId`, nunca pelo `detail`.** O `detail` é texto de log: sai em inglês e
> nomeia o item pelo código interno do SKU (`TSM-000014`). Quem escreve a frase para o cliente é o
> front, que tem nome, cor e tamanho do item no carrinho e sabe em qual tela ele está — no checkout
> a saída é "volte ao carrinho para remover"; no carrinho, o botão de remover está a dois
> centímetros do erro.
>
> **`PRODUCT_UNAVAILABLE` não é estoque zerado.** Os dois mandam `availableQuantity: 0`, mas um item
> volta ao estoque amanhã e o outro saiu de linha. Antes do `reason` eram indistinguíveis, e o teto
> por pedido só se separava dos outros dois por aritmética — inferência que quebrava em silêncio ao
> mudar o limite.

> ### ⚠️ O `401` tem duas formas — e a diferença importa
>
> | Origem | Corpo |
> |---|---|
> | Qualquer rota que exija sessão (`/auth/me`, `/cart`, `/orders`, …) | **vazio, zero bytes** |
> | `POST /auth/refresh` | `ProblemDetail` completo, com `detail` |
>
> O primeiro nasce no entry point do Spring Security, que responde só com o status. Um
> `response.json()` cego nesse caso quebra — **sempre cheque o `Content-Length` ou use try/catch
> antes de desserializar um 401.**
>
> O segundo é emitido pelo próprio controller, e o `detail` dele é a única forma de distinguir
> "a sessão expirou" de "detectamos reuso do seu token e revogamos tudo" ([tabela na §4](#post-apiv1authrefresh--público)). Não descarte esse corpo: é o aviso de segurança mais
> importante do fluxo, e ele muda o que você mostra ao usuário.

### 1.4 Status codes e o que fazer com cada um

| Status | Significado | Ação no front |
|---|---|---|
| `401` | Sem sessão válida | Tentar `POST /auth/refresh`; se falhar, mandar para o login |
| `403` | Autenticado, mas sem permissão — **ou header CSRF ausente** | Mostrar "sem acesso". **Não** renovar sessão |
| `404` | Recurso não existe | Página de não encontrado |
| `409` | Conflito: duplicidade ou estoque | Mostrar `detail`; em estoque, usar `reason`, `skuId` e `availableQuantity` ([§1.3](#13-formato-de-erro-problemdetail-rfc-7807)) |
| `422` | Campos inválidos | Marcar os campos usando `fields` |
| `429` | Rate limit ou conta travada | Mostrar `detail`, desabilitar o botão |
| `500` | Erro do servidor | Mensagem genérica |

> **A distinção 401/403 é crítica.** Se o interceptor tratar 403 como sessão expirada, um usuário sem permissão — ou uma requisição sem o header CSRF — dispara renovação de token em laço e acaba deslogado.

### 1.5 Paginação

Endpoints paginados aceitam `page` (base 0), `size` e `sort` (`campo,asc|desc`). A resposta usa o envelope do Spring Boot 4, com os metadados **aninhados em `page`**:

```json
{
  "content": [ /* ... */ ],
  "page": { "size": 12, "number": 0, "totalElements": 49, "totalPages": 5 }
}
```

> Não é `totalElements` na raiz. Se você já viu APIs Spring antigas, o formato mudou.

**`size` tem teto de 100.** Pedir mais não dá erro — a API devolve 100 e informa isso em `page.size`. Leia o tamanho da resposta em vez de assumir o que você pediu; um `?size=5000` volta com `"size": 100`, e paginar como se fossem 5000 pula registros.

**`sort` é restrito por endpoint.** Cada rota paginada aceita apenas uma lista de campos, e um campo fora dela devolve **400** com o `detail` listando os permitidos — não é ignorado em silêncio. As listas estão documentadas em cada endpoint. Caminho aninhado (`sort=user.email`) nunca é aceito.

### 1.6 Valores monetários

Chegam como número JSON com duas casas (`180.90`). **Não some valores no front** — `subtotal`, `totalPrice`, `totalAmount` já vêm calculados em aritmética decimal exata. Formate com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.

### 1.7 Datas

`LocalDateTime` sem fuso: `"2026-08-06T21:29:21.671002"`. Não têm `Z` nem offset — trate como horário do servidor.

---

## 2. Enums

Use exatamente estes valores; qualquer outro devolve 400.

```ts
type Role           = 'CUSTOMER' | 'ADMIN'
type TargetAudience = 'MEN' | 'WOMEN'
type ProductSize    = 'PP' | 'P' | 'M' | 'G' | 'GG' | 'XG'
type OrderStatus    = 'PENDING_PAYMENT' | 'PAYMENT_FAILED' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
type DisplayPosition= 'HEADER' | 'NEW_ARRIVALS' | 'FEATURED' | 'NONE' | 'HOME_MAIN' | 'HOME_SECONDARY'

type Category = 'JACKETS' | 'COATS_AND_TRENCHES' | 'DRESSES' | 'BLAZERS' | 'SHIRTS_AND_BLOUSES'
              | 'JEANS' | 'T_SHIRTS' | 'SHIRTS' | 'SKIRTS_AND_SHORTS' | 'SHORTS'

type State = 'AC'|'AL'|'AP'|'AM'|'BA'|'CE'|'DF'|'ES'|'GO'|'MA'|'MT'|'MS'|'MG'|'PA'|'PB'
           | 'PR'|'PE'|'PI'|'RJ'|'RN'|'RS'|'RO'|'RR'|'SC'|'SP'|'SE'|'TO'
```

**Categoria depende do público** — o back-end rejeita combinações inválidas com 400:

| Público | Categorias válidas |
|---|---|
| `WOMEN` | `DRESSES`, `JACKETS`, `COATS_AND_TRENCHES`, `SHIRTS_AND_BLOUSES`, `JEANS`, `T_SHIRTS`, `SKIRTS_AND_SHORTS` |
| `MEN` | `JACKETS`, `COATS_AND_TRENCHES`, `BLAZERS`, `T_SHIRTS`, `SHIRTS`, `JEANS`, `SHORTS` |

O endpoint `GET /catalog/products/categories?targetAudience=WOMEN` devolve a lista já filtrada — use-o para popular selects em vez de duplicar essa tabela.

> `State` não inclui `RM`. A lista tem 27 itens, mas confira contra a sua se houver divergência.

---

## 3. Mapa de rotas e permissões

| Prefixo | Acesso |
|---|---|
| `/api/v1/auth/**` | público (exceto `/me`, que exige sessão) |
| `/api/v1/catalog/**` | público |
| `/api/v1/cart/**` | sessão |
| `/api/v1/orders/**` | sessão |
| `/api/v1/addresses/**` | sessão |
| `/api/v1/admin/**` | **`ROLE_ADMIN`** |
| `/api/v1/webhooks/**` | público (assinatura HMAC do Stripe) |

Existe um segundo papel com alcance no painel, `ROLE_ADMIN_VIEWER`, usado pela conta de
demonstração pública. Ele só é aceito em **GET**, e só nestes quatro prefixos:

| Rota | `ROLE_ADMIN` | `ROLE_ADMIN_VIEWER` |
|---|:---:|:---:|
| `GET /api/v1/admin/dashboard` | ✅ | ✅ |
| `GET /api/v1/admin/products/**` | ✅ | ✅ |
| `GET /api/v1/admin/collections/**` | ✅ | ✅ |
| `GET /api/v1/admin/audit` | ✅ | ✅ |
| `GET /api/v1/admin/orders/**` | ✅ | ❌ `403` |
| qualquer escrita (`POST`/`PUT`/`PATCH`/`DELETE`) | ✅ | ❌ `403` |

Pedidos ficaram de fora porque `GET /api/v1/admin/orders` aceita `searchTerm`, que casa por
substring no e-mail e no nome do cliente — um canal de consulta que mascarar a resposta não fecha.

Para o front, a diferença prática é que uma sessão de viewer deve esconder as ações de escrita e o
menu de pedidos: a API recusa de qualquer forma, mas oferecer um botão que sempre devolve `403` é
uma tela quebrada.

---

## 4. Autenticação — `/api/v1/auth`

### 4.1 Implementando a autenticação no front

Esta seção é o roteiro de implementação; as subseções seguintes são o contrato de cada rota.

#### O ciclo de vida da sessão

```
register ──► e-mail com token ──► verify-email ──┐
                                                 ├──► SESSÃO ATIVA
login ───────────────────────────────────────────┘        │
                                                          │ access token expira (15 min)
                                                          ▼
                                                       refresh ──► sessão renovada
                                                          │
                                                          │ refresh recusado
                                                          ▼
                                                        login
```

Dois pontos que costumam surpreender: **`verify-email` já devolve os cookies de sessão** — não peça login depois dele; e **`register` não autentica** — ele só dispara o e-mail.

#### 1. Inicialização: aqueça o cookie CSRF

Toda escrita precisa do header `X-XSRF-TOKEN`, cujo valor vem do cookie `__Host-XSRF-TOKEN`. O cookie é emitido por qualquer resposta que passe pela cadeia de segurança, então **um `GET` qualquer na subida do app já resolve** — `GET /api/v1/catalog/products?size=1` serve.

Sem esse passo, a primeira escrita do usuário (inclusive o próprio login, se você não usar as rotas isentas) sai sem o header e toma **403**.

#### 2. O interceptor: `401` e `403` não são a mesma coisa

É o erro mais caro deste fluxo, e ele não aparece em teste feliz:

| Status | O que significa | O que fazer |
|---|---|---|
| `401` | Não há sessão válida | Tentar `refresh` **uma vez**; se falhar, ir para o login |
| `403` | Há sessão, mas falta permissão — **ou falta o header CSRF** | Mostrar "sem acesso". **Nunca** renovar |

A diferença é conceitual, e é o que explica tudo: `401` responde "não sei quem você é" — renovar credencial faz sentido. `403` responde "sei quem você é, e você não pode" — renovar não muda nada, porque a resposta não depende de a credencial estar fresca.

Tratar `403` como sessão expirada produz um laço. Medido, simulando esse interceptor:

```
ciclo 1: escrita → 403 | interceptor chama refresh → 200
ciclo 2: escrita → 403 | interceptor chama refresh → 200
ciclo 3: escrita → 403 | interceptor chama refresh → 200
```

O refresh **funciona todas as vezes** — a sessão nunca esteve com problema. Sem um limite de retentativas, isso não termina. Três danos, em ordem de gravidade:

1. **O usuário é deslogado por clicar onde não tinha permissão.** Quando o interceptor desiste, manda para o login — conclusão errada tirada de uma sessão perfeitamente boa.
2. **Cada ciclo rotaciona o refresh token.** Se dois ciclos se sobrepuserem, você cai na corrida do item 3, agora com destruição real de sessão.
3. **O erro real fica escondido.** `403` também é a resposta para **header CSRF ausente**. Um endpoint onde você esqueceu o `X-XSRF-TOKEN` não se manifesta como "faltou o header": se manifesta como logout misterioso. O sintoma aponta para o lugar errado, e você depura autenticação quando o problema é uma linha do interceptor.

#### 3. Serialize o refresh numa única promise

**Esta é a parte que quebra em produção e passa em desenvolvimento.** O refresh token **rotaciona**: cada chamada consome o atual e emite outro. Se várias requisições tomarem `401` ao mesmo tempo e cada uma disparar seu próprio refresh, todas competem pelo mesmo token — e só uma pode vencer.

O comportamento real, medido com 5 refreshes simultâneos apresentando o mesmo token:

```
req1 → 401  "Invalid or expired refresh token."
req2 → 401  "Invalid or expired refresh token."
req3 → 200  sessão nova emitida
req4 → 401  "Invalid or expired refresh token."
req5 → 401  "Invalid or expired refresh token."
```

O consumo do token é atômico no servidor, então exatamente uma chamada ganha. As outras chegam no intervalo de microssegundos entre o token ser consumido e a janela de graça ser aberta — não encontram nem o token válido, nem a graça, nem o registro de uso — e recebem o `401` **genérico**, indistinguível de "sua sessão expirou".

> ### ⚠️ O perdedor apaga os cookies que o vencedor acabou de criar
>
> Esta é a parte que faz a corrida virar logout. Comparando os headers das respostas simultâneas:
>
> ```
> req3 (200): Set-Cookie: __Host-access_token=<novo>;  Max-Age=900
> req1 (401): Set-Cookie: __Host-access_token=;        Max-Age=0    ← apaga
> req5 (401): Set-Cookie: __Host-access_token=;        Max-Age=0    ← apaga
> ```
>
> O servidor limpa os cookies quando um token é apresentado e recusado — correto isoladamente, destrutivo na corrida. **Qual resposta chega por último no browser é indeterminado**, então o usuário pode ser deslogado com a sessão válida e recém-renovada.
>
> É o pior tipo de defeito: intermitente, dependente de ordem de chegada, e ausente em desenvolvimento — uma aba, cliques espaçados, nunca duas requisições expirando juntas. Uma tela que dispara seis chamadas paralelas ao montar cai nisso com frequência.

**A janela de graça de 30s não cobre esse caso, e não foi feita para isso.** Ela atende retentativa **sequencial** — resposta perdida, timeout, o mesmo token reapresentado depois que a primeira chamada terminou. Nesse formato ela funciona: o mesmo token usado três vezes em sequência devolve `200` nas três. Ela só não ajuda quem chega antes de ela existir.

**A revogação em massa exige passar dos 30s.** Reapresentar o token depois que a graça expirou devolve `401 "Security Alert: Token reuse detected. All sessions have been revoked."` — o mecanismo funcionando como projetado, para um token que vazou, e não para concorrência do cliente.

A promise compartilhada resolve os três de uma vez: com um refresh por vez, não existe perdedor para apagar cookie nenhum.

```ts
let refreshing: Promise<void> | null = null

function refreshOnce(): Promise<void> {
  // Todas as chamadas concorrentes esperam a MESMA promise.
  refreshing ??= fetch('/api/v1/auth/refresh', { method: 'POST' })
    .then(res => { if (!res.ok) throw res })
    .finally(() => { refreshing = null })

  return refreshing
}
```

No interceptor: ao receber `401`, aguarde `refreshOnce()` e repita a requisição original **uma única vez**. Se o refresh falhar, propague o erro e mande para o login — não tente de novo.

#### 4. Trate os três motivos de falha do refresh

O `401` do refresh **tem corpo** (ao contrário dos demais — ver §1.3), e o `detail` distingue casos que merecem telas diferentes:

| `detail` | O que aconteceu | O que mostrar |
|---|---|---|
| `"No refresh token was provided."` | Nunca houve sessão | Tela de login, sem alarme |
| `"Invalid or expired refresh token."` | Expirou naturalmente | "Sua sessão expirou, entre novamente" |
| `"Security Alert: Token reuse detected. All sessions have been revoked."` | **O token foi usado duas vezes** | Avise explicitamente que todas as sessões foram encerradas por segurança |

O terceiro caso é o único sinal de que uma credencial pode ter vazado. Engoli-lo numa mensagem genérica desperdiça o único momento em que o usuário poderia agir.

#### 5. Cookies e limpeza

Não tente apagar os cookies de sessão pelo JavaScript — eles são `HttpOnly` e o script não os enxerga. Quem os limpa é o servidor:

- `POST /auth/logout` zera os dois e coloca o access token numa denylist, então ele para de valer na hora.
- `POST /auth/refresh` limpa os cookies **só quando um token foi apresentado e recusado**. Chegando sem cookie nenhum, ele responde `401` sem mexer em nada — de propósito: ausência de credencial não é motivo para descartar credencial.

Depois do logout, descarte o estado de usuário em memória. O servidor já invalidou a sessão, mas a sua aplicação continua achando que há alguém logado até você limpar.

#### Checklist da autenticação

- [ ] `GET` de aquecimento na subida, para receber o cookie CSRF
- [ ] Header `X-XSRF-TOKEN` em todo `POST`/`PUT`/`PATCH`/`DELETE`
- [ ] Chamadas em caminho relativo (URL absoluta para o backend quebra a origem única)
- [ ] Interceptor separando `401` de `403`
- [ ] Refresh numa promise compartilhada, com retentativa única
- [ ] `401` do refresh lido pelo `detail`, com mensagem específica para reuso de token
- [ ] `401` das demais rotas tratado **sem** ler o corpo (vem vazio)

---

### `POST /api/v1/auth/register` — público

```ts
{ firstName: string   // 2..50, obrigatório
  lastName:  string   // 2..50, obrigatório
  email:     string   // formato de e-mail, máx 255, obrigatório
  password:  string } // 8..72, obrigatório
```

**201** → `{ "message": "Registration successful. Please check your email to verify your account." }`

Não autentica. Envia um e-mail com link para `{APP_BASE_URL}/verify-email?token=<uuid>` — uma **rota do front**, que deve ler o `token` da URL e chamar o endpoint abaixo. O token vale 24h.

**Não existe erro para "e-mail já cadastrado".** Um endereço que já tem conta recebe exatamente este mesmo `201` e esta mesma mensagem — a rota é pública, e responder `409` ali dizia "esta pessoa é cliente da loja" para quem iterasse uma lista de endereços. O aviso não some, muda de canal: quem já tem conta recebe um e-mail "você já tem uma conta", com link para o login, em vez de um cadastro novo.

Para o front isso significa que **o tratamento de `409` neste formulário virou código morto**. A tela de sucesso passa a ser a única saída do caminho feliz.

Erros: `422` campos · `429` mais de 5 registros por IP em 15 min.

### `POST /api/v1/auth/verify-email` — público

```ts
{ token: string }   // o uuid da URL
```

**200**, corpo vazio, **com os cookies de sessão**. Verificar o e-mail já loga o usuário — não peça login depois.

Erros: `401` token inválido ou expirado.

### `POST /api/v1/auth/resend-verification` — público

```ts
{ email: string }
```

**200** sempre, corpo vazio — inclusive para e-mail inexistente ou já verificado. É deliberado: respostas diferentes permitiriam descobrir quem tem conta na loja. **Não tente inferir nada da resposta.**

Erros: `429` mais de 3 por IP em 15 min.

### `POST /api/v1/auth/login` — público

```ts
{ email: string, password: string }
```

**200** + cookies de sessão. O corpo traz **apenas identificação**:

```json
{ "email": "cliente@example.com", "name": "Nome Sobrenome" }
```

Os tokens **não** aparecem no JSON (`@JsonIgnore`), só nos cookies.

Erros:
- `401` — credenciais inválidas (`"Invalid email or password."`)
- `403` — e-mail não verificado (`"Please verify your email before logging in."`) → ofereça o reenvio
- `429` — 10 tentativas por IP em 15 min, **ou** conta travada após 5 falhas (`detail` informa os minutos)

### `POST /api/v1/auth/refresh` — público

**Sem corpo.** O refresh token vem do cookie. O corpo opcional `{ refreshToken }` que existia aqui foi removido — enviá-lo agora é ignorado.

**200** + cookies novos, mesmo corpo do login.

**401** → os cookies são limpos **apenas quando um token foi apresentado e recusado**. Quando não havia cookie nenhum, o 401 volta sem mexer nos cookies. O `detail` distingue três casos, e vale mostrar mensagens diferentes:

| `detail` | Significado |
|---|---|
| `"No refresh token was provided."` | Sessão nunca existiu |
| `"Invalid or expired refresh token."` | Expirou — login normal. **Também é o que um refresh concorrente perdedor recebe** |
| `"Security Alert: Token reuse detected. All sessions have been revoked."` | **Reuso detectado**: todas as sessões do usuário foram revogadas |

> O refresh **rotaciona** o token: cada chamada consome o atual e emite outro. O consumo é atômico, então entre chamadas simultâneas exatamente uma vence.
>
> A janela de graça de 30s cobre retentativa **sequencial** — resposta perdida, timeout, o mesmo token reapresentado depois que a chamada anterior terminou. Ela **não** cobre chamadas simultâneas, que chegam antes de a graça existir e recebem o `401` genérico da segunda linha da tabela. Só depois dos 30s a reapresentação vira detecção de reuso e revogação.
>
> **Serialize as renovações no front** — [§4.1, item 3](#3-serialize-o-refresh-numa-única-promise), que documenta a corrida com números medidos e explica por que o perdedor apaga os cookies do vencedor.

### `GET /api/v1/auth/me` — sessão

**200**:

```json
{
  "id": "8486e2f9-1821-4e53-bc54-377ec9337ff0",
  "firstName": "Doc", "lastName": "Probe", "name": "Doc Probe",
  "email": "doc-probe@example.com",
  "role": "CUSTOMER"
}
```

Use o `role` para decidir se mostra a área de admin. Erros: `401`.

### `POST /api/v1/auth/logout` — público, **mas exige CSRF**

Sem corpo. **200**, cookies zerados. O access token entra numa denylist e para de valer imediatamente, mesmo antes de expirar.

É público de propósito: exigir sessão válida travaria justamente quem está com o token expirado.

---

## 5. Catálogo — `/api/v1/catalog` (público)

### `GET /api/v1/catalog/products` — busca paginada

| Param | Tipo | Observação |
|---|---|---|
| `searchTerm` | string | nome ou descrição, case-insensitive |
| `category` | `Category` | |
| `targetAudience` | `TargetAudience` | |
| `collectionId` | number | |
| `minPrice` / `maxPrice` | decimal | **sobre o preço efetivo** (promocional quando existe) |
| `isFeatured` | boolean | destaque editorial |
| `onSale` | boolean | `true` = só em promoção; `false` = só sem promoção |
| `page` / `size` / `sort` | | padrão `size=12`, teto de `size=100` |

Só retorna produtos ativos e não removidos.

Ordenação restrita a `name`, `price`, `promotionalPrice` e `createdAt`. Qualquer outro campo em `?sort=` devolve **400** listando os permitidos — inclusive caminho aninhado como `collection.name`.

**200** → `Page<ProductSummary>`:

```json
{
  "content": [{
    "id": 1,
    "name": "CAMISA VESTIDO",
    "slug": "camisa-vestido",
    "price": 299.90,
    "promotionalPrice": 180.90,
    "featured": true,
    "coverImageUrl": "https://res.cloudinary.com/.../capa.jpg",
    "hoverImageUrl": "https://res.cloudinary.com/.../hover.jpg",
    "colorsHex": ["#221713"],
    "active": true
  }],
  "page": { "size": 12, "number": 0, "totalElements": 49, "totalPages": 5 }
}
```

`promotionalPrice` nulo = sem promoção. Os dois preços vêm juntos para o card montar o "de/por" sem requisição extra.

> **`?sort=price,asc` ordena pelo preço de tabela, não pelo efetivo.** O filtro usa o efetivo, a ordenação não — um produto de R$ 299,90 em promoção por R$ 180,90 pode aparecer depois de um de R$ 200. Não reordene no client: a lista é paginada e reordenar a página atual produz uma ordem que muda ao virar a página. É pendência de API.

### `GET /api/v1/catalog/products/{id}` e `GET /api/v1/catalog/products/slug/{slug}`

**200** → `ProductResponse`:

```ts
{
  id: number, name: string, slug: string, description: string | null,
  fabricCompositions: { material: Material, label: string, percentage: number }[],
  careInstructions: { instruction: CareInstruction, label: string, axis: CareAxis }[],
  price: number, promotionalPrice: number | null,
  collection: CollectionResponse | null,
  category: Category, targetAudience: TargetAudience,
  active: boolean, featured: boolean,
  colors: {
    id: number, colorName: string, colorHex: string,
    coverImageUrl: string | null, hoverImageUrl: string | null,
    galleryImages: string[],
    skus: { id: number, size: ProductSize, skuCode: string, stockQuantity: number }[]
  }[]
}
```

Cores e SKUs removidos são filtrados na resposta do catálogo — e o `deletedAt` não existe mais neste contrato, justamente porque era sempre nulo. O `version` do SKU também saiu: é o token de bloqueio otimista, e só o painel o usa, na contagem de inventário. O `AdminProductResponse` é quem carrega os dois.

> **A rota por slug devolve `200` ou `404` — nunca redireciona.** O slug é gerado na criação, a
> partir do nome, e **não muda quando o produto é renomeado**: o link publicado continua valendo para
> sempre. Use o `slug` que a resposta traz, sem montá-lo no cliente e sem acrescentar o id.

### `GET /api/v1/catalog/products/categories?targetAudience=WOMEN`

**200** → `["DRESSES", "JACKETS", ...]`

### `GET /api/v1/catalog/products/materials`

**200** → `{ name: Material, label: string }[]` — o vocabulário fechado de composição.
`name` é o que vai em `fabricCompositions[].material`; `label` é o texto em português,
para exibir. Não mantenha a tradução no cliente: é o mesmo vocabulário em dois lugares,
que é o que o enum acabou de fechar.

### `GET /api/v1/catalog/products/care-instructions`

**200** → `{ axis: CareAxis, label: string, options: { name: CareInstruction, label: string }[] }[]`

Vem **agrupado por eixo**, e o agrupamento é o contrato: cada eixo aceita uma única
instrução. Monte o formulário como um campo por eixo — um multi-select das dezesseis
opções deixa marcar "Não lavar" junto de "Lavar à mão", e a API recusa com `400`.

Os eixos são `WASH`, `BLEACH`, `TUMBLE_DRY`, `NATURAL_DRY`, `IRON` e `PROFESSIONAL`.
Secadora e secagem natural são separados de propósito: uma etiqueta real diz
"não usar secadora" **e** "secar à sombra".

### `GET /api/v1/catalog/collections`

Params: `position` (`DisplayPosition`), `targetAudience`. **200** → **lista simples, não paginada**:

```ts
{ id: number, name: string, slug: string, description: string | null,
  active: boolean, heroImageUrl: string | null, portraitImageUrl: string | null,
  squareImageUrl: string | null, displayPosition: DisplayPosition,
  displayOrder: number, targetAudience: TargetAudience }[]
```

### `GET /api/v1/catalog/collections/{id}` e `GET /api/v1/catalog/collections/slug/{slug}`

Mesmo objeto. A rota por slug segue a mesma regra do produto: `200` ou `404`, sem redirect.

---

## 6. Carrinho — `/api/v1/cart` (sessão)

Todos os endpoints retornam o **carrinho inteiro** — substitua o estado local pela resposta, não faça merge.

```ts
type CartResponse = {
  id: number,
  items: {
    id: number,            // id do item no carrinho, usado em update/remove
    skuId: number, skuCode: string, size: string,
    productId: number, productName: string, productSlug: string,
    colorName: string, coverImageUrl: string,
    quantity: number,
    unitPrice: number,     // preço efetivo, já com promoção aplicada
    subtotal: number,
    stockQuantity: number, // estoque atual do SKU
    available: boolean     // false = produto saiu do ar; bloqueie o checkout
  }[],
  totalItems: number,
  totalPrice: number
}
```

> `unitPrice` já é o preço promocional quando existe. O item **não** traz o preço de tabela, então não há como exibir o riscado dentro do carrinho hoje. Não busque o produto de novo para derivá-lo: o preço pode ter mudado no meio, e você mostraria um desconto diferente do que está sendo cobrado.

| Método | Rota | Corpo | Notas |
|---|---|---|---|
| `GET` | `/api/v1/cart` | — | |
| `POST` | `/api/v1/cart/items` | `{ skuId: number, quantity: 1..10 }` | soma à quantidade existente |
| `PUT` | `/api/v1/cart/items/{itemId}` | `{ quantity: 1..10 }` | define a quantidade |
| `DELETE` | `/api/v1/cart/items/{itemId}` | — | |
| `POST` | `/api/v1/cart/sync` | `{ items: [{ skuId, quantity }] }` | mescla um carrinho local após o login |
| `DELETE` | `/api/v1/cart` | — | **204**, sem corpo |

**Limite de 10 unidades por item.** No `POST /items`, ultrapassar dá `409`; no `/sync`, a quantidade é **silenciosamente reduzida** ao limite ou ao estoque disponível.

O `/sync` também **ignora em silêncio** SKUs inexistentes ou fora de estoque, em vez de falhar. Compare a resposta com o que você enviou para avisar o usuário do que não entrou.

Erros: `401` · `404` SKU ou item inexistente · `409` estoque (com `reason`, `skuId` e `availableQuantity`) · `422` campos.

---

## 7. Pedidos — `/api/v1/orders` (sessão)

### `POST /api/v1/orders/checkout`

```ts
{ addressId: number, items: [{ skuId: number, quantity: number }] }
```

> Os itens vêm no corpo, **não** do carrinho salvo. Monte a lista a partir do carrinho antes de chamar.

**201** → `OrderResponse` com `clientSecret` preenchido — é o único momento em que ele aparece. Passe-o ao Stripe.js para confirmar o pagamento.

```ts
type OrderResponse = {
  id: number,
  status: OrderStatus,
  totalAmount: number,
  shippingFee: number,        // hoje fixo em 0.00
  clientSecret: string | null,
  shippingAddress: { street, number, complement, neighborhood,
                     city, state, postalCode },  // congelado no pedido
  expiresAt: string,          // 30 minutos após a criação
  createdAt: string,
  items: {
    id: number, skuId: number, productName: string, skuCode: string,
    size: string, color: string, imageUrl: string,
    priceAtPurchase: number,      // o que foi cobrado
    listPriceAtPurchase: number,  // preço de tabela naquele momento
    quantity: number
  }[]
}
```

O pedido congela os dois preços. Para mostrar o desconto no histórico:

```ts
const hadDiscount = item.listPriceAtPurchase > item.priceAtPurchase
```

Nunca compare com o preço **atual** do produto — ele pode ter mudado depois da compra, e você inventaria um desconto que nunca existiu.

> **O estoque é reservado no checkout e o pedido expira em 30 minutos** (`expiresAt`). Se o pagamento não for confirmado, um scheduler cancela e devolve o estoque. Mostre esse prazo ao usuário.

Erros: `401` · `404` endereço ou SKU · `409` estoque (com `reason`, `skuId` e `availableQuantity`) · `422` campos.

### `GET /api/v1/orders/my-orders`

Paginado, padrão `size=10`, ordenado por `createdAt desc`. `clientSecret` vem nulo.

Ordenação restrita a `createdAt`, `totalAmount` e `status`. Outro campo devolve **400** — inclusive caminho aninhado, que antes era aceito.

### `GET /api/v1/orders/{id}`

**200** → `OrderResponse`. Pedido de outro usuário devolve **403**.

### Status do pedido

```
PENDING_PAYMENT → PAID → SHIPPED → DELIVERED
       ↓
  PAYMENT_FAILED / CANCELLED
```

O front **não** muda status — quem faz isso é o webhook do Stripe (pagamento) ou o admin (envio/entrega). Depois de confirmar o pagamento no Stripe.js, faça polling em `GET /orders/{id}` até o status sair de `PENDING_PAYMENT`: a confirmação chega ao back-end pelo webhook, de forma assíncrona.

---

## 8. Endereços — `/api/v1/addresses` (sessão)

```ts
type AddressRequest = {
  street: string,        // obrigatório, máx 255
  number: string,        // obrigatório, máx 10
  complement?: string,   // máx 255
  neighborhood: string,  // obrigatório, máx 100
  city: string,          // obrigatório, máx 100
  state: State,          // obrigatório
  postalCode: string,    // "12345678" ou "12345-678"
  isDefault: boolean
}
```

A resposta acrescenta `id`. O `postalCode` é **normalizado para 8 dígitos** ao salvar — a API aceita com hífen e devolve sem.

| Método | Rota | Retorno |
|---|---|---|
| `GET` | `/api/v1/addresses` | lista simples |
| `POST` | `/api/v1/addresses` | **201** |
| `PUT` | `/api/v1/addresses/{id}` | **200** |
| `PATCH` | `/api/v1/addresses/{id}/default` | **200**, sem corpo de requisição |
| `DELETE` | `/api/v1/addresses/{id}` | **204** |

**Máximo de 5 endereços por usuário** — o sexto devolve `422` com `"You can only have up to 5 addresses."`. Só um pode ser padrão; marcar um novo desmarca o anterior automaticamente.

Erros: `401` · `404` `"Address not found."` · `422` limite ou campos.

---

## 9. Área administrativa — `/api/v1/admin` (`ROLE_ADMIN`)

Um `CUSTOMER` autenticado recebe **403** aqui. Trate como "sem acesso", não como sessão expirada.

### Produtos

| Método | Rota | |
|---|---|---|
| `POST` | `/api/v1/admin/products` | **201** |
| `GET` | `/api/v1/admin/products` | mesmos filtros do catálogo + inativos e removidos; `size=20` |
| `GET` | `/api/v1/admin/products/{id}` | inclui removidos |
| `PUT` | `/api/v1/admin/products/{id}` | **200** |
| `DELETE` | `/api/v1/admin/products/{id}` | **204**, remoção lógica |
| `POST` | `/api/v1/admin/products/{id}/restore` | **200**, desfaz a remoção — volta **inativo** |

Ordenação restrita a `id`, `name`, `price`, `promotionalPrice`, `category`, `targetAudience`, `active`, `featured`, `createdAt`, `updatedAt`. Qualquer outro campo em `?sort=` devolve **400** listando os permitidos.

> ### As rotas de admin devolvem tipos próprios
>
> `AdminProductResponse` e `AdminProductSummary` **não são** o `ProductResponse` e o `ProductSummary` do catálogo. O que têm a mais é escrituração interna, e não dado sigiloso:
>
> | Campo | Onde | Para quê |
> |---|---|---|
> | `deletedAt` | produto, cor, SKU e card | marcar removidos na busca do admin e oferecer a restauração |
> | `version` | SKU | a contagem de inventário do `PATCH /admin/skus/{id}/stock` |
>
> A separação existe pelo contrato, não pelo sigilo: enquanto um record servia os dois públicos, um campo de custo ou margem acrescentado para o painel aparecia na vitrine no mesmo commit. No cliente, gere dois tipos — não estenda um do outro.

```ts
type ProductRequest = {
  name: string,                  // obrigatório, máx 255
  description?: string,          // máx 5000
  fabricCompositions?: { material: Material, percentage: number }[],
  careInstructions?: CareInstruction[],  // no máximo uma por eixo
  price: number,                 // obrigatório, > 0, máx 8 dígitos + 2 decimais
  promotionalPrice?: number,     // > 0 e < price
  collectionId?: number,
  category: Category,            // obrigatório
  targetAudience: TargetAudience,// obrigatório
  active: boolean,
  featured: boolean,
  colors: {                      // obrigatório, ao menos uma
    id?: number,                 // presente = atualiza; ausente = cria
    colorName: string,           // máx 100
    colorHex: string,            // #RGB ou #RRGGBB
    coverImageUrl?: string,      // máx 500
    hoverImageUrl?: string,      // máx 500
    galleryImages?: string[],
    skus: { id?: number,
            size: ProductSize,
            stockQuantity?: number }[] // >= 0; SÓ para SKU novo (ver abaixo)
  }[]
}
```

> ### ⚠️ O `PUT` é substituição total
>
> Campo ausente é tratado como remoção. Um formulário que envie só os campos alterados **apaga a promoção, a coleção e as cores omitidas** — e responde `200`, sem erro.
>
> Cores e SKUs que não aparecerem na lista são **removidos**. Envie sempre o objeto completo, carregado de `GET /admin/products/{id}`, com os `id` preservados.

> ### ⚠️ O `PUT` não mexe em estoque
>
> `stockQuantity` só é aceito em SKU **novo** (sem `id`), como estoque inicial. Enviá-lo junto com um `id` devolve **400** apontando a rota certa — recusa explícita, e não um `200` para uma alteração que não aconteceu.
>
> Ao montar o payload a partir de `GET /admin/products/{id}`, **remova `stockQuantity` de todo SKU que tenha `id`**. Estoque se ajusta pelo [`PATCH /admin/skus/{id}/stock`](#estoque).
>
> Essa separação é o que faz o formulário de produto parar de competir com o checkout: uma edição de descrição salva enquanto o produto vendia passa sem 409 e sem desfazer a venda.

Regras específicas, todas com `400` e `detail` explicativo:
- soma das composições ≠ 100%
- material repetido na composição
- duas instruções de cuidado do mesmo eixo (por exemplo `DO_NOT_WASH` com `HAND_WASH`)
- `promotionalPrice >= price`
- categoria incompatível com o público
- `stockQuantity` em SKU existente, ou ausente em SKU novo
- `?sort=` com campo fora da lista permitida

Outros erros: `409` nome de produto duplicado · `422` campos.

> **`skuCode` não vai no request.** O backend o gera no formato `TSM-000123` e o devolve na resposta,
> somente-leitura. Ele é opaco de propósito: no checkout o código é copiado para dentro do pedido e a
> partir dali é imutável, enquanto nome do produto, nome da cor e tamanho continuam editáveis — um
> código descritivo viraria uma afirmação falsa no primeiro rename, e um código congelado num pedido
> não tem para onde redirecionar. Nunca monte o código no cliente, e não o envie de volta no `PUT`.

**Restauração.** `POST /api/v1/admin/products/{id}/restore` traz de volta o produto, suas cores e seus SKUs — sem ressuscitar o que tinha sido removido antes, numa edição anterior. O produto volta com `active: false`: recuperar o cadastro e republicar na vitrine são duas decisões. Erros: `400` se o produto não está removido · `409`, hoje inalcançável pela API, se algum `skuCode` tiver sido ocupado nesse meio-tempo — os códigos saem da sequência e nunca se repetem, então isso só acontece com escrita direta no banco.

### Dashboard

`GET /api/v1/admin/dashboard?lowStockThreshold=5&lowStockPage=0` → **200**

Tudo o que a home do painel precisa, numa requisição.

```ts
type DashboardResponse = {
  ordersByStatus: Record<OrderStatus, number>,   // TODOS os status, inclusive zerados
  revenue: { today: number, last7Days: number, last30Days: number },
  lowStock: { skuId: number, skuCode: string,
              productId: number, productName: string,
              colorName: string, size: ProductSize,
              stockQuantity: number,
              version: number }[],   // o token de bloqueio otimista do SKU — ver nota
  lowStockCount: number,             // total, além da amostra
  lowStockPageSize: number,          // tamanho da amostra (20)
  lowStockPage: number               // página devolvida, ecoando o parâmetro
}
```

`lowStockThreshold` é opcional (padrão `5`, máximo `1000`); negativo ou acima do teto devolve **400**.

`lowStockPage` é opcional (padrão `0`) e pagina **apenas a lista de estoque baixo** — o resto da resposta é recalculado igual. Negativo devolve **400**. Use-o com `lowStockCount` e `lowStockPageSize` para navegar o alerta inteiro sem mudar de tela.

> **Cada linha de `lowStock` já traz o `version`**, e não é redundância: o ajuste por contagem física (`absolute`) do [`PATCH /admin/skus/{id}/stock`](#estoque) **exige** esse valor. Com ele aqui, a tela ajusta o estoque direto da linha do alerta, sem um `GET` do produto só para descobrir a versão.

> **`ordersByStatus` traz todos os status**, com zero onde não há pedido. Não trate chave ausente — ela não acontece.

> **`lowStock` é uma amostra de até 20 linhas; `lowStockCount` é o total.** Mostre "20 de 47", não "20". A lista vem ordenada do estoque menor para o maior, e cada linha traz o `skuId` para a tela oferecer o `PATCH /admin/skus/{id}/stock` direto dali.

> **Só produtos ativos e não removidos entram no alerta de estoque.** Um produto fora da vitrine não perde venda, e listá-lo transformaria o alerta num inventário de rascunhos.

> ### ⚠️ `revenue` é valor de pedido, não dinheiro liquidado
>
> Somam-se apenas pedidos em `PAID`, `SHIPPED` e `DELIVERED` — `PENDING_PAYMENT` ainda não é dinheiro, `PAYMENT_FAILED` nunca foi, `CANCELLED` sai da conta.
>
> Só que **cancelar um pedido pago não estorna nada** hoje, então esse cancelamento tira o valor daqui sem tirar o dinheiro da Stripe. Enquanto o estorno não existir, o número é o que a loja deve reconhecer, e não o que ela recebeu — rotule a tela de acordo.

As janelas contam **dias inteiros a partir da meia-noite**: `today` é de hoje 00:00 em diante, `last7Days` cobre hoje e os seis anteriores. Não são janelas móveis de 24h — dois acessos à mesma tela no mesmo dia mostram o mesmo número.

### Estoque

`PATCH /api/v1/admin/skus/{id}/stock` → **200**

Duas operações, exclusivas entre si. Envie exatamente uma.

```ts
type StockAdjustment =
  | { delta: number,      reason: StockChangeReason, absolute?: never, version?: never }
  | { absolute: number, version: number, reason: StockChangeReason, delta?: never }

type StockChangeReason =
  | 'RESTOCK'          // chegou mercadoria
  | 'INVENTORY_COUNT'  // contagem física
  | 'RETURN'           // devolução que volta a ser vendável
  | 'DAMAGE'
  | 'LOSS'
  | 'CORRECTION'       // erro de cadastro, sem movimento físico

type StockResponse = {
  skuId: number, skuCode: string,
  stockQuantity: number,  // valor já aplicado
  version: number         // já incrementado — use este no próximo `absolute`
}
```

**Use `delta` sempre que puder.** Ele não precisa saber o total, então não precisa de versão e não colide com vendas simultâneas: dois ajustes concorrentes somam em vez de se sobrescreverem. `delta: 0` é recusado.

**`absolute` é para contagem física**, e por isso exige o `version` que o `GET` do produto devolveu para aquele SKU. Se o estoque tiver se movido desde a leitura, a resposta é **409** (`title: "Stale data"`) dizendo a quantidade e a versão atuais — recarregue e confirme a contagem antes de salvar.

> Não calcule `delta = contado − exibido` no cliente para simular a contagem. A conta sairia de uma leitura possivelmente vencida, que é exatamente o problema que o `version` existe para pegar.

Erros: `400` resultado negativo (o `detail` traz o disponível atual) · `404` SKU inexistente ou fora do catálogo · `409` versão vencida · `422` payload com as duas formas, com nenhuma, com `absolute` sem `version`, ou sem `reason`.

### Coleções

| Método | Rota | Retorno |
|---|---|---|
| `POST` | `/api/v1/admin/collections` | **201** |
| `GET` | `/api/v1/admin/collections` | lista simples, **inclui inativas** |
| `GET` | `/api/v1/admin/collections/{id}` | **200** |
| `PUT` | `/api/v1/admin/collections/{id}` | **200** |
| `DELETE` | `/api/v1/admin/collections/{id}?cascadeProducts=false` | **204** |
| `POST` | `/api/v1/admin/collections/{id}/restore` | **200**, desfaz a remoção — volta **inativa**, **sem destaque** e **sem produtos** |

```ts
type CollectionRequest = {
  name: string,             // obrigatório, máx 255
  active: boolean,
  description?: string,     // máx 5000
  heroImageUrl?: string, portraitImageUrl?: string, squareImageUrl?: string,  // máx 255
  displayPosition?: DisplayPosition,
  displayOrder?: number,
  targetAudience: TargetAudience   // obrigatório
}
```

> ### Excluir coleção: o padrão **desassocia**, não apaga
>
> `DELETE /api/v1/admin/collections/{id}` remove a coleção e deixa os produtos dela sem coleção — eles continuam no catálogo.
>
> `?cascadeProducts=true` apaga cada produto junto (remoção lógica). Confirme com o admin antes, mostrando quantos produtos serão afetados.

Posições de destaque são exclusivas: um único `HOME_MAIN` no site inteiro, e um `HEADER`/`HOME_SECONDARY` por público. Conflito devolve `409`.

> ### ⚠️ Restaurar coleção: os produtos **não** voltam junto
>
> `POST /api/v1/admin/collections/{id}/restore` traz de volta a coleção, e só ela.
>
> - Se a exclusão foi a padrão, os produtos foram **desassociados** — o vínculo deixou de existir, e não há o que restaurar. A coleção volta vazia.
> - Se foi em cascata, os produtos continuam removidos. Cada um precisa do seu `POST /api/v1/admin/products/{id}/restore`.
>
> A coleção volta com `active: false` **e `displayPosition: NONE`** — a remoção libera a posição de destaque, e outra coleção pode tê-la ocupado no intervalo. Reativar e reatribuir o destaque são duas edições, nessa ordem. Erros: `400` se ela não está removida · `404` se o id nunca existiu.
>
> **A coleção removida some de todas as listagens** — diferente do produto, que continua aparecendo na busca do admin. O caminho para descobrir o id é a mensagem de conflito de nome, abaixo.

> ### O nome e o slug continuam ocupados depois da remoção
>
> As constraints de nome e slug de coleção são **totais**, e não parciais como a de `skuCode`: uma coleção removida não solta nenhum dos dois. Recriar "Verão 26" para `WOMEN` depois de excluí-la devolve **409** — e o `detail` traz o id da coleção removida e a rota de restauração, em vez do genérico "A data conflict occurred".
>
> O lado bom disso: restaurar **nunca** falha por conflito, porque nada foi liberado no intervalo. É o inverso exato do produto, cujo `skuCode` é liberado na remoção e por isso pode estar tomado na volta.

### Pedidos

- `GET /api/v1/admin/orders` — paginado, `size=20`, `createdAt desc`
- `GET /api/v1/admin/orders/{id}` — detalhe
- `PATCH /api/v1/admin/orders/{id}/status?newStatus=SHIPPED` — o status vai em **query param**, não no corpo

Ordenação restrita a `id`, `status`, `totalAmount`, `createdAt`, `updatedAt`, `expiresAt`. Outro campo em `?sort=` devolve **400**.

**Filtros da listagem**, todos opcionais e combináveis:

| Parâmetro | | |
|---|---|---|
| `status` | `OrderStatus` | |
| `searchTerm` | texto | id do pedido, e-mail ou nome do comprador |
| `createdFrom` | `YYYY-MM-DD` | inclusivo |
| `createdTo` | `YYYY-MM-DD` | inclusivo — **o dia inteiro entra** |

`GET /api/v1/admin/orders?status=PAID&searchTerm=maria@&createdFrom=2026-08-01&createdTo=2026-08-11`

> `searchTerm` é uma caixa única, e não três. Um termo numérico casa com o **id exato** do pedido (quem digita "12" quer o pedido 12, não os pedidos 12, 112 e 120) e, ao mesmo tempo, com e-mail e nome — "2024" é um id plausível e um pedaço de e-mail plausível, então os dois conjuntos vêm juntos em vez de a ambiguidade devolver nada.

> `createdTo` inclui o dia inteiro: um pedido feito às 14h do último dia do intervalo entra. Intervalo invertido (`createdFrom > createdTo`) devolve **400** em vez de uma lista vazia — vazio seria lido como "não há pedidos no período".

> ### ⚠️ O detalhe do pedido para o admin mudou de rota
>
> Era `GET /api/v1/orders/{id}`, a mesma rota do cliente. Um ADMIN chegando lá agora recebe **403** — use `GET /api/v1/admin/orders/{id}`, que devolve mais informação, e não menos: a resposta traz a identificação do comprador, que a visão do cliente nunca teve.

As três rotas de admin devolvem `AdminOrderResponse`, que **não é** o `OrderResponse` do cliente:

```ts
type AdminOrderResponse = {
  id: number, status: OrderStatus,
  totalAmount: number, shippingFee: number,
  customerId: string, customerName: string, customerEmail: string,  // não existem no OrderResponse
  shippingAddress: ShippingAddress,
  expiresAt: string | null, createdAt: string,
  items: OrderItem[]
  // sem clientSecret: é credencial de pagamento do cliente, e o painel não usa
}
```

`GET /api/v1/orders/{id}` é agora exclusivo do dono do pedido e sempre devolve `OrderResponse` completo. Antes ele servia os dois públicos e anulava o `clientSecret` para o admin em tempo de execução — a ausência da credencial no painel é agora garantida pelo tipo, que simplesmente não tem o campo.

Transições aceitas — espelhe este mapa na interface em vez de oferecer todos os status:

| De | Para |
|---|---|
| `PENDING_PAYMENT` | `PAID`, `PAYMENT_FAILED`, `CANCELLED` |
| `PAYMENT_FAILED` | `PAID`, `CANCELLED` |
| `PAID` | `SHIPPED`, `CANCELLED` |
| `SHIPPED` | `DELIVERED` |
| `DELIVERED` · `CANCELLED` | terminais |

Transição fora do mapa devolve **400** com `title: "Invalid status transition"` e as propriedades `from` e `to` no corpo.

> ⚠️ **Cancelar um pedido `PAID` devolve o estoque e não estorna nada.** Não existe integração de reembolso no back-end. A confirmação na interface precisa dizer isso com todas as letras.

### Upload de imagens

`POST /api/v1/admin/uploads` — `multipart/form-data`, campo `files` (múltiplos), campo opcional `folder` (padrão `general`).

**201** → `{ "urls": ["https://res.cloudinary.com/..."] }`

Aceita apenas `image/jpeg`, `image/png` e `image/webp`, **validando a assinatura binária do arquivo**, não só o `Content-Type`. Máx 5 MB por arquivo, 20 MB por requisição.

Erros: `415` formato inválido · `413` acima do limite · `400` arquivo vazio.

### Histórico de alterações

`GET /api/v1/admin/audit` → **200**, paginado, `size=20`, `createdAt desc`

Uma linha por alteração administrativa: quem fez, o quê, em qual registro e quando.

```ts
type AuditLogResponse = {
  id: number,
  actor: string,                 // e-mail de quem estava logado, ou "system"
  entityType: AuditedEntity,
  entityId: string,              // string, não number — ver nota abaixo
  action: AuditAction,
  previousValue: string | null,  // preenchidos aos pares, só nas ações de mudança de campo
  newValue: string | null,
  reason: StockChangeReason | null,  // só em STOCK_ADJUSTED
  details: string | null,        // texto livre: código do SKU, modo da exclusão de coleção
  createdAt: string
}

type AuditedEntity = 'PRODUCT' | 'PRODUCT_SKU' | 'COLLECTION' | 'ORDER'

type AuditAction =
  | 'CREATED' | 'UPDATED' | 'DELETED' | 'RESTORED'
  | 'STATUS_CHANGED'              // pedido; previous/new são OrderStatus
  | 'STOCK_ADJUSTED'              // SKU; previous/new são quantidades, e reason vem preenchido
  | 'PROMOTIONAL_PRICE_CHANGED'   // produto; previous/new são preços, e null significa "sem promoção"
```

**Filtros**, todos opcionais e combináveis:

| Parâmetro | | |
|---|---|---|
| `entityType` | `AuditedEntity` | |
| `entityId` | texto | **casamento exato** |
| `actor` | texto | casa por trecho, sem diferenciar maiúsculas |
| `action` | `AuditAction` | |
| `createdFrom` | `YYYY-MM-DD` | inclusivo |
| `createdTo` | `YYYY-MM-DD` | inclusivo — o dia inteiro entra |

`GET /api/v1/admin/audit?entityType=PRODUCT&entityId=42` é o histórico de um produto, pronto para virar uma aba na tela de edição. Intervalo invertido devolve **400**. Ordenação restrita a `id`, `createdAt`, `actor`, `action`, `entityType`.

> **Só leitura.** Não existe rota para criar, editar ou apagar uma linha de auditoria, e a tabela é imutável no banco. Não construa tela de edição aqui.

> **`entityId` é string.** As quatro entidades de hoje usam id numérico, mas a coluna guarda texto para caber o `UUID` de usuário quando a promoção a `ADMIN` virar rota. Compare como string, não converta.

> **`previousValue`/`newValue` são `null` nas ações que não mudam um campo** — `CREATED`, `UPDATED`, `DELETED` e `RESTORED` deixam os dois vazios. E `null` em `PROMOTIONAL_PRICE_CHANGED` significa **ausência de promoção**, não valor desconhecido: `null → "149.90"` é uma promoção criada, `"149.90" → null` é uma promoção retirada.

> **Uma edição de produto com mudança de preço gera duas linhas**, `UPDATED` e `PROMOTIONAL_PRICE_CHANGED`. São perguntas diferentes: "alguém salvou o formulário" e "a promoção mudou". Agrupe por `createdAt` se a tela precisar mostrá-las como um evento só.

> **O que não aparece aqui:** a transição para `PAID` vinda do webhook do Stripe, e os campos alterados num `UPDATED`. O histórico de um pedido começa na primeira ação manual, e uma edição de produto registra que houve edição, não o diff.

---

## 10. Webhook do Stripe

`POST /api/v1/webhooks/stripe` — chamado pelo Stripe, nunca pelo front. Não implemente nada aqui.

---

## 11. Checklist de implementação

- [ ] Rewrite de `/api/*` no `next.config.js`, ativo em **dev e produção**, com toda chamada em caminho relativo
- [ ] Header `X-XSRF-TOKEN` em toda escrita, lido do cookie `__Host-XSRF-TOKEN`
- [ ] `GET` de aquecimento na inicialização para receber o cookie CSRF
- [ ] Interceptor que trata **401** com refresh e **403** como falta de permissão — nunca os dois iguais
- [ ] Refresh serializado numa única promise compartilhada
- [ ] Tratar 401 sem tentar ler o corpo (vem vazio)
- [ ] Ler paginação de `response.page.*`, não da raiz
- [ ] Formulários exibindo erros a partir de `fields`, incluindo chaves indexadas (`items[0].quantity`)
- [ ] Formulário de produto do admin enviando o objeto **completo** no `PUT`
- [ ] Polling do status do pedido após confirmar o pagamento
- [ ] Exibir `expiresAt` do pedido (janela de 30 min)

## 12. Limitações conhecidas da API

Coisas que **não** existem — não assuma nem contorne no front:

1. **Ordenação por preço efetivo.** `sort=price` usa o preço de tabela.
2. **Preço de tabela no carrinho.** Não há como riscar o preço no carrinho.
3. **Frete.** `shippingFee` é sempre `0.00`; não há cálculo por CEP.
4. **Cupons, agendamento de promoção, promoção por SKU ou por coleção.**
5. **Recuperação de senha.** Não existe endpoint de "esqueci minha senha".
6. **Edição de perfil.** `GET /auth/me` é leitura; não há update de nome ou senha.
7. **Percentual de desconto.** Calcule no front: `Math.round((price - promotionalPrice) / price * 100)`.
