# Painel administrativo — especificação de implementação para o front-end

Documento de referência para implementar o painel `/admin`. Contém **todos** os endpoints sob
`/api/v1/admin/**`, todos os DTOs de entrada e saída, e o comportamento de cada verbo.

Escrito para ser lido por inteiro antes de escrever código. Muitas regras aqui são contraintuitivas
e o servidor **não** as corrige por você — ele responde erro, e alguns desses erros só aparecem em
produção com dados reais.

- Autenticação, cookies, CSRF e refresh: [`API_REFERENCE.md` §4.1](API_REFERENCE.md#41-implementando-a-autenticação-no-front).
- Contrato completo da API, incluindo rotas públicas: [`API_REFERENCE.md`](API_REFERENCE.md).

---

## 1. Antes de qualquer endpoint

### 1.1 As seis regras que quebram a integração

Leia estas seis antes de escrever a primeira tela. Cada uma corresponde a um erro que o servidor
devolve e que não é óbvio pelo nome do endpoint.

1. **O `PUT` de produto e de coleção é substituição total.** Campo ausente significa remoção, e a
   resposta é `200`. Um formulário que envie só o que mudou apaga o resto em silêncio.
2. **O `PUT` de produto não aceita `stockQuantity` em SKU existente.** Estoque tem endpoint próprio.
   Enviá-lo com `id` preenchido devolve `400`.
3. **Excluir coleção não apaga os produtos** — desassocia. A cascata é opt-in por query param.
4. **Restaurar coleção não traz os produtos de volta.** Nunca, em nenhum dos dois modos de exclusão.
5. **Toda escrita exige o header `X-XSRF-TOKEN`.** Sem ele, `403` — não `401`.
6. **`403` nunca significa sessão expirada.** Se o interceptor tratar `403` como token vencido, ele
   entra em laço de refresh e desloga um usuário cuja sessão estava perfeita.

### 1.2 Origem única — não é opcional

O front e a API **precisam** responder pelo mesmo hostname, via rewrite do Next. Não há CORS
configurado no backend, de propósito.

```js
// next.config.js
async rewrites() {
  return [{ source: '/api/v1/:path*', destination: `${process.env.API_URL}/api/v1/:path*` }]
}
```

**Use o rewrite também em desenvolvimento.** Chamar `http://localhost:8080` direto de
`localhost:3000` é cross-origin e não funciona — e o erro não vai parecer um erro de CORS, porque a
requisição morre antes de chegar à aplicação.

Consequência prática: **sempre caminho relativo** (`/api/v1/admin/products`), nunca URL absoluta.

### 1.3 Cliente HTTP

```ts
import axios from 'axios'

export const api = axios.create({
  baseURL: '/api/v1',
  xsrfCookieName: '__Host-XSRF-TOKEN',   // o padrão do axios é 'XSRF-TOKEN' e não acha este cookie
  xsrfHeaderName: 'X-XSRF-TOKEN',
})
```

O `xsrfCookieName` não é detalhe: sem ele o axios procura um cookie chamado `XSRF-TOKEN`, que não
existe, e toda escrita volta `403`.

Os cookies de sessão são `HttpOnly` e vão sozinhos porque a origem é a mesma —
`withCredentials` é desnecessário.

> O cookie `__Host-XSRF-TOKEN` é emitido em qualquer resposta que passe pela cadeia de segurança. Um
> `GET` qualquer na inicialização já basta; na prática o `GET /auth/me` do bootstrap resolve.

### 1.4 Descobrir que o usuário é admin

```ts
type UserResponse = {
  id: string, firstName: string, lastName: string,
  name: string, email: string, role: 'CUSTOMER' | 'ADMIN' | 'ADMIN_VIEWER'
}
```

`GET /api/v1/auth/me` → `200` com o objeto acima. Renderize o painel para `ADMIN` e `ADMIN_VIEWER`.

> **`ADMIN_VIEWER` abre o painel em somente leitura** — alcança apenas os `GET` de dashboard,
> produtos, coleções e auditoria, e recebe `403` em toda escrita e em pedidos. A matriz completa
> está em [`API_REFERENCE.md` §3](API_REFERENCE.md#3-mapa-de-rotas-e-permissões). Para este papel,
> esconda o menu de pedidos e as ações de escrita — inclusive os contadores do dashboard, que linkam
> para `/admin/orders`. Um portão escrito como `role === 'ADMIN'` expulsa o papel do painel logo
> após o login, que é o único desfecho em que ele não serve para nada.

> **Isto é UX, não segurança.** A autorização real é do servidor, por prefixo de path. Esconder o
> menu não protege nada; deixar de esconder também não expõe nada.

> **O papel viaja no JWT.** Promover alguém a `ADMIN` no banco não tem efeito até o próximo login —
> o token em circulação continua dizendo `CUSTOMER` até expirar.

> **Para desenvolver, use a conta semeada por migration:** `admin@tsm-atelier.com` / `senha123`, já
> com o e-mail verificado. O cadastro público (`POST /api/v1/auth/register`) cria sempre `CUSTOMER`,
> e não existe rota para promover ninguém — essa é a única porta de entrada do painel.
>
> `senha123` vale só em desenvolvimento: o hash entra na migration por um placeholder do Flyway, e
> em produção vem da variável `ADMIN_PASSWORD_HASH`. Se o painel não logar num ambiente publicado, a
> senha é outra — peça a quem fez o deploy, não presuma que a semente falhou.

### 1.5 Formato de erro

Todo erro é um `ProblemDetail` (RFC 7807):

```ts
type ProblemDetail = {
  status: number
  title: string
  detail: string
  instance: string
  fields?: Record<string, string>   // só em 422
  from?: OrderStatus, to?: OrderStatus  // só em transição de status inválida
  availableQuantity?: number        // só em conflito de estoque
}
```

| Status | Significado no painel | O que fazer |
|---|---|---|
| `400` | Regra de negócio violada | Mostrar `detail`. É texto para humano, escrito para ser mostrado |
| `401` | Sem sessão | Tentar refresh; se falhar, login |
| `403` | Sem permissão **ou header CSRF ausente** | "Sem acesso". **Nunca** renovar sessão |
| `404` | Recurso inexistente | Voltar para a listagem |
| `409` | Conflito: duplicidade ou dado vencido | Mostrar `detail`; em alguns casos ele contém a ação de saída |
| `413` | Arquivo acima do limite | Upload |
| `415` | Formato de arquivo não suportado | Upload |
| `422` | Campos inválidos | Marcar campo a campo usando `fields` |
| `500` | Erro do servidor | Mensagem genérica |

> **`400` e `422` são coisas diferentes aqui.** `422` traz `fields` e é erro de formulário. `400` é
> regra de negócio — composição que não soma 100%, categoria incompatível com o público, estoque que
> ficaria negativo — e vem sem `fields`, com a explicação em `detail`.

### 1.6 Paginação

```ts
type Page<T> = {
  content: T[]
  page: { size: number, number: number, totalElements: number, totalPages: number }
}
```

Metadados **aninhados em `page`**, não na raiz. `page` é base 0.

### 1.7 Ordenação é restrita por whitelist

`?sort=campo,asc|desc`. Campo fora da lista permitida devolve **400** listando os aceitos — não é
ignorado em silêncio.

| Endpoint | Campos aceitos |
|---|---|
| `GET /admin/products` | `id`, `name`, `price`, `promotionalPrice`, `category`, `targetAudience`, `active`, `featured`, `createdAt`, `updatedAt` |
| `GET /admin/orders` | `id`, `status`, `totalAmount`, `createdAt`, `updatedAt`, `expiresAt` |

Monte os seletores de ordenação a partir dessas listas.

### 1.8 Enums

```ts
type Role            = 'CUSTOMER' | 'ADMIN'
type TargetAudience  = 'MEN' | 'WOMEN'
type ProductSize     = 'PP' | 'P' | 'M' | 'G' | 'GG' | 'XG'
type OrderStatus     = 'PENDING_PAYMENT' | 'PAYMENT_FAILED' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
type DisplayPosition = 'HEADER' | 'NEW_ARRIVALS' | 'FEATURED' | 'NONE' | 'HOME_MAIN' | 'HOME_SECONDARY'
type StockChangeReason = 'RESTOCK' | 'INVENTORY_COUNT' | 'RETURN' | 'DAMAGE' | 'LOSS' | 'CORRECTION'

type Category = 'JACKETS' | 'COATS_AND_TRENCHES' | 'DRESSES' | 'BLAZERS' | 'SHIRTS_AND_BLOUSES'
              | 'JEANS' | 'T_SHIRTS' | 'SHIRTS' | 'SKIRTS_AND_SHORTS' | 'SHORTS'
```

**Categoria depende do público.** O servidor rejeita a combinação inválida com `400`:

| Público | Categorias válidas |
|---|---|
| `WOMEN` | `DRESSES`, `JACKETS`, `COATS_AND_TRENCHES`, `SHIRTS_AND_BLOUSES`, `JEANS`, `T_SHIRTS`, `SKIRTS_AND_SHORTS` |
| `MEN` | `JACKETS`, `COATS_AND_TRENCHES`, `BLAZERS`, `T_SHIRTS`, `SHIRTS`, `JEANS`, `SHORTS` |

Não duplique essa tabela no cliente. Use `GET /api/v1/catalog/products/categories?targetAudience=WOMEN`,
que devolve `Category[]` já filtrado, e repopule o select quando o público mudar. **Trocar o público
com uma categoria já selecionada pode invalidar a seleção** — limpe o campo nesse evento.

### 1.8.1 Composição e cuidados

Os dois campos deixaram de ser texto livre, pelo mesmo motivo: digitados peça a peça, o vocabulário
derrapava — "Algodão" e "Algodao" eram dois materiais para a chave primária, e a mesma fibra entrava
duas vezes no produto sem que nada reclamasse.

| Campo | Endpoint | Formato |
|---|---|---|
| `fabricCompositions[].material` | `GET /catalog/products/materials` | `{ name, label }[]` — select simples |
| `careInstructions[]` | `GET /catalog/products/care-instructions` | agrupado por eixo — **um campo por eixo** |

Cuidados vêm agrupados porque **cada eixo aceita uma única instrução**. Um multi-select das dezesseis
opções deixa marcar "Não lavar" junto de "Lavar à mão", e aí a API recusa com `400` — a etiqueta
contraditória é um erro pior que o de digitação, porque lê como instrução legítima. Renderize seis
campos (`WASH`, `BLEACH`, `TUMBLE_DRY`, `NATURAL_DRY`, `IRON`, `PROFESSIONAL`), todos opcionais.

Secadora e secagem natural são eixos separados de propósito: uma etiqueta real diz "não usar secadora"
**e** "secar à sombra". Não os junte num campo só.

Envie sempre a constante (`name` / `options[].name`), nunca o `label` — o rótulo é apresentação, e vem
na resposta só para você não manter a tradução de novo no cliente.

### 1.9 Datas e dinheiro

- Datas: `LocalDateTime` sem fuso — `"2026-08-11T09:23:34.075576"`. Sem `Z`, sem offset. Trate como
  horário do servidor; não converta.
- Filtros de data: `LocalDate`, formato `YYYY-MM-DD`.
- Dinheiro: número JSON com duas casas (`180.90`). **Não some no cliente** — todos os totais já vêm
  calculados em aritmética decimal exata.

---

## 2. Mapa completo das rotas de admin

Tudo sob `/api/v1/admin/**` exige `ROLE_ADMIN`. A autorização é **posicional** — vale pelo prefixo do
path, não por anotação em método.

| # | Método | Rota | Sucesso |
|---|---|---|---|
| **Dashboard** ||||
| 1 | `GET` | `/api/v1/admin/dashboard` | `200` |
| **Produtos** ||||
| 2 | `GET` | `/api/v1/admin/products` | `200` `Page<AdminProductSummary>` |
| 3 | `GET` | `/api/v1/admin/products/{id}` | `200` `AdminProductResponse` |
| 4 | `POST` | `/api/v1/admin/products` | `201` `AdminProductResponse` |
| 5 | `PUT` | `/api/v1/admin/products/{id}` | `200` `AdminProductResponse` |
| 6 | `DELETE` | `/api/v1/admin/products/{id}` | `204` |
| 7 | `POST` | `/api/v1/admin/products/{id}/restore` | `200` `AdminProductResponse` |
| **Estoque** ||||
| 8 | `PATCH` | `/api/v1/admin/skus/{id}/stock` | `200` `StockResponse` |
| **Coleções** ||||
| 9 | `GET` | `/api/v1/admin/collections` | `200` `CollectionResponse[]` |
| 10 | `GET` | `/api/v1/admin/collections/{id}` | `200` `CollectionResponse` |
| 11 | `POST` | `/api/v1/admin/collections` | `201` `CollectionResponse` |
| 12 | `PUT` | `/api/v1/admin/collections/{id}` | `200` `CollectionResponse` |
| 13 | `DELETE` | `/api/v1/admin/collections/{id}` | `204` |
| 14 | `POST` | `/api/v1/admin/collections/{id}/restore` | `200` `CollectionResponse` |
| **Pedidos** ||||
| 15 | `GET` | `/api/v1/admin/orders` | `200` `Page<AdminOrderResponse>` |
| 16 | `GET` | `/api/v1/admin/orders/{id}` | `200` `AdminOrderResponse` |
| 17 | `PATCH` | `/api/v1/admin/orders/{id}/status` | `200` `AdminOrderResponse` |
| **Upload** ||||
| 18 | `POST` | `/api/v1/admin/uploads` | `201` `{ urls: string[] }` |
| **Auditoria** ||||
| 19 | `GET` | `/api/v1/admin/audit` | `200` `Page<AuditLogResponse>` |

> **Nenhuma rota de admin fora deste prefixo.** O detalhe de pedido para o admin é o item 16 — a rota
> `GET /api/v1/orders/{id}` é exclusiva do dono do pedido e devolve **403** para um ADMIN que não seja
> o comprador. Ela devolve menos informação, não mais.

---

## 3. Produtos

O recurso mais complexo do painel: aninhamento de três níveis — produto → cores → SKUs — e `PUT` de
substituição total.

### 3.1 Tipos

#### Request (usado no `POST` e no `PUT`)

```ts
type ProductRequest = {
  name: string                    // obrigatório, 1..255
  description?: string            // máx 5000
  fabricCompositions?: { material: Material, percentage: number }[]
  careInstructions?: CareInstruction[]  // no máximo uma por eixo
  price: number                   // obrigatório, > 0, máx 8 dígitos inteiros + 2 decimais
  promotionalPrice?: number       // > 0 e estritamente < price; ausente = sem promoção
  collectionId?: number           // ausente = sem coleção
  category: Category              // obrigatório
  targetAudience: TargetAudience  // obrigatório
  active: boolean
  featured: boolean
  colors: ProductColorRequest[]   // obrigatório, ao menos uma
}

type ProductColorRequest = {
  id?: number                     // presente = atualiza; ausente = cria
  colorName: string               // obrigatório, máx 100
  colorHex: string                // obrigatório, /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/
  coverImageUrl?: string          // máx 500
  hoverImageUrl?: string          // máx 500
  galleryImages?: string[]        // cada item máx 500
  skus: ProductSKURequest[]       // obrigatório, ao menos um
}

type ProductSKURequest = {
  id?: number                     // presente = atualiza; ausente = cria
  size: ProductSize               // obrigatório
  stockQuantity?: number          // >= 0. SÓ para SKU novo. Ver 3.3
}
// Sem skuCode: o backend o gera (TSM-000123). Vem na resposta, somente-leitura.
```

#### Response

```ts
type AdminProductResponse = {
  id: number
  name: string
  slug: string
  description: string | null
  fabricCompositions: { material: Material, label: string, percentage: number }[]
  careInstructions: { instruction: CareInstruction, label: string, axis: CareAxis }[]
  price: number
  promotionalPrice: number | null
  collection: CollectionResponse | null
  category: Category
  targetAudience: TargetAudience
  active: boolean
  featured: boolean
  colors: AdminProductColorResponse[]
  deletedAt: string | null        // não-nulo = produto removido logicamente
}

type AdminProductColorResponse = {
  id: number
  colorName: string
  colorHex: string
  coverImageUrl: string | null
  hoverImageUrl: string | null
  galleryImages: string[]
  skus: AdminProductSKUResponse[]
  deletedAt: string | null
}

type AdminProductSKUResponse = {
  id: number
  version: number                 // token de bloqueio otimista — ver 4.2
  size: ProductSize
  skuCode: string
  stockQuantity: number
  deletedAt: string | null
}

type AdminProductSummary = {
  id: number
  name: string
  slug: string
  price: number
  promotionalPrice: number | null
  featured: boolean
  coverImageUrl: string | null    // capa da primeira cor
  hoverImageUrl: string | null
  colorsHex: string[]             // para a bolinha de cores no card
  deletedAt: string | null        // não-nulo = removido; ofereça "restaurar" no lugar de "editar"
  active: boolean
}
```

> **Os tipos de admin não são os do catálogo.** `ProductResponse` e `ProductSummary` das rotas
> públicas não têm `deletedAt` nem o `version` do SKU. Gere os dois conjuntos separadamente; não
> estenda um do outro, porque eles divergem de propósito.

### 3.2 `GET /api/v1/admin/products` — listagem

Query params, todos opcionais:

| Parâmetro | Tipo | |
|---|---|---|
| `searchTerm` | `string` | nome ou descrição, parcial, case-insensitive |
| `category` | `Category` | |
| `targetAudience` | `TargetAudience` | |
| `collectionId` | `number` | |
| `minPrice` / `maxPrice` | `number` | aplicados sobre o **preço efetivo** (promocional quando existe) |
| `isFeatured` | `boolean` | |
| `onSale` | `boolean` | `true` = tem `promotionalPrice`; `false` = não tem |
| `page` / `size` / `sort` | | padrão `size=20`, ordenação restrita (ver 1.7) |

**A busca do admin é a única que enxerga produtos inativos e removidos.** A listagem pública filtra
os dois. Renderize `deletedAt !== null` com tratamento visual distinto e troque a ação primária de
"editar" para "restaurar" — editar um produto removido devolve `404`.

> **`?sort=price` ordena pelo preço de tabela, não pelo efetivo.** O filtro de faixa usa o efetivo, a
> ordenação não. Um produto de R$ 299,90 em promoção por R$ 180,90 aparece depois de um de R$ 200.
> Não reordene a página no cliente: a lista é paginada, e reordenar a página atual produz uma ordem
> que muda ao virar a página.

### 3.3 `POST` e `PUT` — o formulário

#### O `PUT` é substituição total

O corpo do `PUT` é o **estado final desejado** do produto inteiro. Isso vale para:

- **Campos escalares.** `promotionalPrice` ausente retira a promoção. `collectionId` ausente
  desassocia da coleção. `description` ausente apaga a descrição. E responde `200`.
- **Cores.** Cor que não aparecer na lista é **removida**, com todos os seus SKUs.
- **SKUs.** SKU que não aparecer na lista da sua cor é **removido**.

Consequência para o formulário: carregue o produto com `GET /admin/products/{id}` e envie o objeto
completo de volta, preservando todos os `id`.

**Perder um `id` no estado do formulário significa criar duplicata**, não atualizar. `id` presente
atualiza; `id` ausente cria.

#### Estoque não é editável aqui

```
❌ { "id": 12, "size": "M", "stockQuantity": 10 }   → 400
✅ { "id": 12, "size": "M" }                        → ok
✅ { "size": "M", "stockQuantity": 10 }             → ok (SKU novo)
```

Ao montar o payload a partir da resposta do `GET`, **remova `stockQuantity` de todo SKU que tenha
`id`**. Em SKU novo o campo é obrigatório — é o estoque inicial.

O `400` traz no `detail` a rota certa, com o id do SKU já preenchido.

> **Por que isso é bom para você:** enquanto o formulário escrevia estoque, salvar uma correção de
> descrição num produto que estava vendendo devolvia `409`, e a única saída era recarregar e refazer
> a edição. Com estoque fora deste payload, essa corrida não existe mais.

#### Transformação recomendada

```ts
function toRequest(p: AdminProductResponse): ProductRequest {
  return {
    name: p.name,
    description: p.description ?? undefined,
    // A resposta traz o rótulo junto, e o request não o quer de volta: os dois
    // campos abaixo descartam o que é apresentação e mantêm só a constante.
    fabricCompositions: p.fabricCompositions.map(f => ({
      material: f.material,
      percentage: f.percentage,
    })),
    careInstructions: p.careInstructions.map(c => c.instruction),
    price: p.price,
    promotionalPrice: p.promotionalPrice ?? undefined,
    collectionId: p.collection?.id,
    category: p.category,
    targetAudience: p.targetAudience,
    active: p.active,
    featured: p.featured,
    colors: p.colors.map(c => ({
      id: c.id,
      colorName: c.colorName,
      colorHex: c.colorHex,
      coverImageUrl: c.coverImageUrl ?? undefined,
      hoverImageUrl: c.hoverImageUrl ?? undefined,
      galleryImages: c.galleryImages,
      skus: c.skus.map(s => ({
        id: s.id,
        size: s.size,
        // skuCode e stockQuantity omitidos de propósito: o código é gerado pelo
        // backend e somente-leitura, e estoque tem porta própria (3.3).
      })),
    })),
  }
}
```

Este round-trip — `GET` → `toRequest` → `PUT` sem alterar nada — **tem** que devolver `200`. É a
verificação mais barata de que o formulário está correto, e é o primeiro teste a escrever.

#### Validações que o cliente deve espelhar

Replique-as no schema do formulário para o usuário não descobrir no submit:

| Regra | Erro |
|---|---|
| `name` obrigatório, máx 255 | `422` |
| `price` obrigatório, > 0, máx 8 inteiros + 2 decimais | `422` |
| `promotionalPrice` estritamente menor que `price` | `400` |
| Ao menos uma cor | `422` |
| Cada cor com ao menos um SKU | `422` |
| `colorHex` no formato hex | `422` |
| Composição de tecido soma **exatamente** 100% | `400` |
| Composição sem material repetido | `400` |
| Categoria compatível com o público | `400` |
| `stockQuantity` ausente em SKU novo, ou presente em SKU existente | `400` |

> **Composição de tecido é opcional, mas se enviada tem que somar 100.** Uma lista vazia ou ausente
> passa; uma lista com 98% não.

### 3.4 `DELETE /api/v1/admin/products/{id}` → `204`

Remoção **lógica**. O produto sai do catálogo, é desativado, e suas cores e SKUs vão junto.

Continua aparecendo na busca do admin com `deletedAt` preenchido — é reversível pelo item 7.

### 3.5 `POST /api/v1/admin/products/{id}/restore` → `200`

Traz de volta o produto, suas cores e seus SKUs.

- **Volta com `active: false`.** Recuperar o cadastro e devolvê-lo à vitrine são duas decisões. A
  interface deve deixar isso explícito e oferecer a publicação como um segundo passo.
- **Não ressuscita o que foi removido antes.** Uma cor apagada numa edição anterior à exclusão do
  produto continua apagada — a restauração desfaz aquela exclusão, e não o histórico inteiro.

| Erro | Quando |
|---|---|
| `400` | O produto não está removido |
| `409` | Algum `skuCode` foi ocupado por outro produto nesse meio-tempo — o `detail` lista os códigos |

O `409` aqui é acionável: mostre os códigos em conflito e leve o admin a renomeá-los no outro
produto antes de tentar de novo.

---

## 4. Estoque

### 4.1 `PATCH /api/v1/admin/skus/{id}/stock` → `200`

Duas operações **exclusivas entre si**. Envie exatamente uma.

```ts
type StockAdjustment =
  | { delta: number,    reason: StockChangeReason }                    // movimento
  | { absolute: number, version: number, reason: StockChangeReason }   // contagem

type StockResponse = {
  skuId: number
  skuCode: string
  stockQuantity: number   // valor já aplicado
  version: number         // JÁ INCREMENTADO — use este no próximo `absolute`
}
```

`reason` é obrigatório nas duas formas. Ofereça como `<select>`; é o que dá sentido ao log quando o
número não bate depois.

### 4.2 Qual das duas usar

**`delta` é o padrão do dia a dia.** Use sempre que a ação for "entrou" ou "saiu": chegou
mercadoria, peça danificada, devolução. Ele não precisa saber o total, não precisa de `version`, e
dois ajustes simultâneos **somam** em vez de se sobrescreverem.

**`absolute` é só para contagem física.** "Tem 7 na prateleira" é um número que não se deriva do
estado atual, e se o sistema discordar alguém precisa saber **antes** de gravar. Por isso exige o
`version` que o `GET` devolveu para aquele SKU.

> ### ⚠️ Não calcule `delta = contado − exibido`
>
> É tentador simular a contagem com um delta e evitar o `version`. Não faça: a conta sairia de uma
> leitura possivelmente vencida, e você reintroduz exatamente o erro que o `version` existe para
> pegar — só que agora invisível, porque o servidor não tem como detectá-lo.

### 4.3 Erros

| Erro | Quando | O que a tela faz |
|---|---|---|
| `400` | O ajuste deixaria o estoque negativo | `detail` traz o disponível atual. Mostre e mantenha o formulário aberto |
| `404` | SKU inexistente ou fora do catálogo | O produto foi removido — recarregue |
| `409` | `version` vencida (só no `absolute`) | **Não é um toast.** Ver abaixo |
| `422` | As duas formas juntas, nenhuma, `absolute` sem `version`, `delta: 0`, ou `reason` ausente | Erro de montagem do payload |

O `409` de contagem merece tratamento próprio: o `detail` traz a quantidade e a versão atuais. Mostre
a divergência — *"o sistema diz 9, você contou 7"* — e ofereça reconferir. Um toast genérico faz o
operador clicar em salvar de novo, o que vai falhar de novo.

### 4.4 Interação recomendada

O ajuste de estoque não pertence ao formulário de produto. Coloque-o na **linha do SKU**, numa tabela
que pode viver dentro da página do produto ou no alerta do dashboard:

- Salva sozinho, com `PATCH`, sem passar pelo `PUT` do produto.
- Atualiza a linha com o `stockQuantity` e o `version` que a resposta devolve — **sem refazer o
  `GET`** do produto inteiro.
- Guarde o `version` retornado. Ele é o que a próxima contagem daquela mesma tela vai enviar.

---

## 5. Coleções

### 5.1 Tipos

```ts
type CollectionRequest = {
  name: string                     // obrigatório, máx 255
  active: boolean
  description?: string             // máx 5000
  heroImageUrl?: string            // máx 255
  portraitImageUrl?: string        // máx 255
  squareImageUrl?: string          // máx 255
  displayPosition?: DisplayPosition
  displayOrder?: number            // ordena a listagem pública, crescente. NÃO é único
  targetAudience: TargetAudience   // obrigatório
}

type CollectionResponse = {
  id: number
  name: string
  slug: string                     // gerado pelo servidor
  description: string | null
  active: boolean
  heroImageUrl: string | null
  portraitImageUrl: string | null
  squareImageUrl: string | null
  displayPosition: DisplayPosition
  displayOrder: number | null
  targetAudience: TargetAudience
}
```

> Note que os limites das URLs de imagem são **255** aqui, e **500** no produto. Não unifique.

**Os três campos de publicação são independentes.** `active` tira da vitrine sem apagar nada;
`displayPosition` diz *onde* na home; `displayOrder` diz *em que ordem* dentro da listagem pública,
crescente. Uma coleção `active: false` não aparece na loja mesmo ocupando `HOME_MAIN`.

`displayOrder` **não tem unicidade**: duas coleções podem ter `1`, e aí a ordem entre elas fica
indefinida. Se a ordem importa para o admin, a interface é que precisa garantir números distintos —
o servidor aceita repetido sem reclamar.

### 5.2 `GET /api/v1/admin/collections` → `CollectionResponse[]`

Lista simples, **sem paginação**, e inclui coleções inativas.

**Não inclui coleções removidas** — elas somem de toda listagem. Isso é diferente de produto, e tem
consequência direta na interface (ver 5.5).

**E não vem ordenada.** Esta rota não aplica ordenação nenhuma; quem ordena por `displayOrder` é a
listagem pública. Para o painel mostrar a mesma sequência que o cliente vê, ordene no cliente por
`displayOrder`.

### 5.3 `POST` e `PUT`

O `PUT` é substituição total, com a mesma semântica do produto: campo ausente é removido.

#### Posições de destaque são exclusivas, e o servidor resolve de dois jeitos diferentes

| Posição | Escopo da exclusividade | Conflito |
|---|---|---|
| `HOME_MAIN` | uma no site inteiro | **Rebaixa a anterior para `NONE`, em silêncio** |
| `HOME_SECONDARY` | uma por público | **Rebaixa a anterior para `NONE`, em silêncio** |
| `HEADER` | uma por público | **Devolve `409`** |
| `NEW_ARRIVALS`, `FEATURED`, `NONE` | sem exclusividade | — |

Essa assimetria é real e a interface deve compensá-la: ao escolher `HOME_MAIN` ou `HOME_SECONDARY`,
avise que a coleção que ocupa a posição hoje será rebaixada, e diga qual é. O servidor faz isso sem
avisar, e o admin descobre depois que uma vitrine ficou vazia.

`name` + `targetAudience` também é único; duplicata devolve `409`.

### 5.4 `DELETE /api/v1/admin/collections/{id}?cascadeProducts=false` → `204`

> ### ⚠️ O padrão **desassocia**; a cascata é opt-in
>
> - **`?cascadeProducts=false` (padrão, ou omitido):** a coleção é removida e os produtos dela ficam
>   sem coleção. Continuam no catálogo, à venda.
> - **`?cascadeProducts=true`:** cada produto da coleção é removido logicamente junto.

A cascata é a única operação verdadeiramente destrutiva do painel. Peça confirmação explícita
mostrando **quantos produtos serão apagados** — o número você já tem, filtrando a listagem de
produtos por `collectionId`.

### 5.5 `POST /api/v1/admin/collections/{id}/restore` → `200`

> ### ⚠️ Os produtos **não** voltam junto — em nenhum dos dois casos
>
> - Se a exclusão foi a padrão, os produtos foram **desassociados**: o vínculo deixou de existir e
>   não há o que restaurar. A coleção volta **vazia**.
> - Se foi em cascata, os produtos continuam removidos. Cada um precisa do seu próprio
>   `POST /admin/products/{id}/restore`.
>
> A coleção volta com `active: false`, como o produto — **e com `displayPosition: NONE`**.
>
> A posição de destaque não volta, e isso não é descuido: a exclusão libera a posição, então outra
> coleção pode tê-la ocupado nesse intervalo. Devolvê-la ocupada quebraria no índice do banco, longe
> de onde a decisão foi tomada.
>
> **Diga isso na tela.** Sem o aviso, o admin restaura a coleção que era `HOME_MAIN`, vê a home
> continuar vazia e não tem como ligar uma coisa à outra. O caminho de volta é uma edição: reativar e
> reatribuir a posição, nessa ordem.

| Erro | Quando |
|---|---|
| `400` | A coleção não está removida |
| `404` | O id nunca existiu |

#### O problema de descoberta, e como a interface resolve

A coleção removida **não aparece em listagem nenhuma**. O único caminho até o id dela é o `409` de
nome duplicado:

```
POST /admin/collections { name: "Verão 26", targetAudience: "WOMEN" }
→ 409 "Collection already exists: Verão 26 for WOMEN (deleted collection 7 still holds
   this name; restore it with POST /api/v1/admin/collections/7/restore, or pick another name)"
```

Isso acontece porque as constraints de nome e slug são **totais**: uma coleção removida continua
ocupando os dois.

**Trate esse `409` como uma ação, não como texto de erro.** Extraia o id do `detail` e ofereça um
botão — *"existe uma coleção removida com este nome. Restaurar?"*. Sem isso, o admin fica preso: não
consegue criar, não vê o que está bloqueando, e não tem como chegar à rota de restauração.

> O lado bom da mesma regra: restaurar coleção **nunca** falha por conflito, porque nada foi liberado
> no intervalo. O produto tinha o problema oposto enquanto o `skuCode` era digitado; com o código
> saindo de uma sequência, que nunca repete um número, a restauração também não colide mais.

---

## 6. Pedidos

O painel **não cria nem exclui** pedidos. Ele lista, consulta e move status.

### 6.1 Tipos

```ts
type AdminOrderResponse = {
  id: number
  status: OrderStatus
  totalAmount: number
  shippingFee: number
  customerId: string           // UUID
  customerName: string         // "Maria Silva", já concatenado
  customerEmail: string
  shippingAddress: ShippingAddress
  expiresAt: string | null     // prazo de pagamento; só relevante em PENDING_PAYMENT
  createdAt: string
  items: OrderItem[]
  // não existe clientSecret aqui: é credencial de pagamento do cliente
}

type ShippingAddress = {
  street: string, number: string, complement: string | null,
  neighborhood: string, city: string, state: string,  // UF, 2 letras
  postalCode: string                                   // 8 dígitos, sem máscara
}

type OrderItem = {
  id: number
  skuId: number | null         // null quando o SKU saiu do catálogo depois da compra
  productName: string          // congelado no momento da compra
  skuCode: string
  size: string
  color: string
  imageUrl: string
  priceAtPurchase: number      // preço efetivamente cobrado
  listPriceAtPurchase: number  // preço de tabela na época — permite mostrar o riscado
  quantity: number
}
```

> **Os campos do item são congelados na compra.** `productName`, `priceAtPurchase` e `imageUrl` são
> cópias do que existia naquele momento, não referências ao produto atual. Exiba-os como estão; ir
> buscar o produto para "atualizar" mostraria dados que não correspondem ao que o cliente comprou.

> **`skuId` pode ser `null`.** Não construa link para o produto sem checar.

### 6.2 `GET /api/v1/admin/orders` — listagem

| Parâmetro | Tipo | |
|---|---|---|
| `status` | `OrderStatus` | |
| `searchTerm` | `string` | id do pedido, e-mail ou nome do comprador |
| `createdFrom` | `YYYY-MM-DD` | inclusivo |
| `createdTo` | `YYYY-MM-DD` | inclusivo — **o dia inteiro entra** |
| `page` / `size` / `sort` | | padrão `size=20`, `createdAt desc` |

**`searchTerm` é uma caixa única, não três campos.** Ela casa com id do pedido, e-mail e nome ao
mesmo tempo. Um termo numérico casa com o **id exato** — quem digita "12" quer o pedido 12, não os
pedidos 12, 112 e 120 — e simultaneamente com e-mail e nome, porque "2024" é um id plausível e um
pedaço de e-mail plausível.

Placeholder sugerido: `nº do pedido, e-mail ou nome`.

Duas armadilhas do filtro de datas, que a interface deve absorver em vez de repassar:

- **`createdTo` já inclui o dia inteiro.** Não some um dia no cliente. Um seletor que envie a mesma
  data nos dois campos devolve aquele dia corretamente.
- **Intervalo invertido devolve `400`** com `from` e `to` no `detail`. Prefira impedir a seleção no
  próprio componente — o erro existe porque uma lista vazia seria lida como "não há pedidos no
  período", que é uma resposta errada apresentada como certa.

### 6.3 `GET /api/v1/admin/orders/{id}` → `AdminOrderResponse`

O detalhe do pedido para o painel.

> **Não use `GET /api/v1/orders/{id}`.** Essa é a rota do cliente: um ADMIN recebe `403` nela, e
> mesmo se recebesse não teria a identificação do comprador.

### 6.4 `PATCH /api/v1/admin/orders/{id}/status?newStatus=SHIPPED` → `AdminOrderResponse`

**O status vai na query string, não no corpo.** O corpo é vazio.

#### Transições permitidas

Espelhe este mapa na interface. Ofereça apenas os destinos válidos para o status atual, em vez de
listar todos e deixar o admin descobrir no `400`.

| De | Para |
|---|---|
| `PENDING_PAYMENT` | `PAID`, `PAYMENT_FAILED`, `CANCELLED` |
| `PAYMENT_FAILED` | `PAID`, `CANCELLED` |
| `PAID` | `SHIPPED`, `CANCELLED` |
| `SHIPPED` | `DELIVERED` |
| `DELIVERED` | — terminal |
| `CANCELLED` | — terminal |

Transição fora do mapa devolve `400` com `title: "Invalid status transition"` e as propriedades
`from` e `to` no corpo — dá para tratar sem inspecionar o texto.

Enviar o **mesmo status** que o pedido já tem é aceito e não faz nada. Duplo clique não vira erro.

#### Efeitos colaterais que a confirmação deve declarar

**Cancelar devolve o estoque reservado ao catálogo.** Isso vale para qualquer origem de
cancelamento.

> ### ⚠️ Cancelar um pedido `PAID` **não estorna o pagamento**
>
> O estoque volta, o status muda, e **o dinheiro continua com a loja**. Não há integração de refund.
>
> O diálogo de confirmação precisa dizer isso com todas as letras — algo como *"o valor não será
> estornado automaticamente; faça o estorno pelo painel da Stripe"*. É o efeito mais caro do painel
> inteiro, e o backend não protege contra ele.

O front **não** move status para `PAID`: quem faz isso é o webhook da Stripe. Os movimentos que o
painel realmente opera são `PAID → SHIPPED` e `SHIPPED → DELIVERED`, mais os cancelamentos.

---

## 7. Dashboard

### 7.1 `GET /api/v1/admin/dashboard?lowStockThreshold=5&lowStockPage=0` → `200`

```ts
type DashboardResponse = {
  ordersByStatus: Record<OrderStatus, number>
  revenue: { today: number, last7Days: number, last30Days: number }
  lowStock: {
    skuId: number, skuCode: string,
    productId: number, productName: string,
    colorName: string, size: ProductSize,
    stockQuantity: number,
    version: number          // habilita a contagem física direto da linha — ver 7.2
  }[]
  lowStockCount: number      // total abaixo do limiar, em todas as páginas
  lowStockPageSize: number   // linhas por página — não repita esta constante no cliente
  lowStockPage: number       // página devolvida, base 0
}
```

`lowStockThreshold` é opcional — padrão `5`, máximo `1000`. Negativo ou acima do teto devolve `400`.

`lowStockPage` é opcional, padrão `0`, base 0. Negativo devolve `400`; página além do fim devolve
`lowStock: []` com o `lowStockCount` verdadeiro — a tela não deve concluir daí que o alerta acabou.

> **`lowStock` era uma amostra fixa dos vinte primeiros, sem paginação.** A resposta anunciava "20 de
> 37" e não havia caminho até os outros dezessete: mexer no limiar corta pelo lado errado, porque a
> lista sobe do menor estoque para o maior e não existe piso. `lowStockPage` foi acrescentado por
> isso. É aditivo e com default, então quem chamava sem ele continua recebendo a primeira página.

> **`version` também não existia aqui.** Sem ela a linha só conseguia oferecer `delta` — "entrou 3",
> "saiu 2" —, e a tela que lista justamente os SKUs que alguém vai conferir na prateleira não sabia
> registrar o resultado da conferência. Como §4.2 proíbe simular a contagem com `delta = contado −
> exibido`, a alternativa era mandar o operador procurar o produto em outra tela. O campo é aditivo.

### 7.2 Como renderizar cada bloco

**`ordersByStatus` traz todos os status**, com `0` onde não há pedido. Não escreva código para chave
ausente — ela não acontece. Cada contador deve linkar para a listagem já filtrada:
`/admin/orders?status=PENDING_PAYMENT`.

**`lowStock` é uma página de `lowStockPageSize` linhas; `lowStockCount` é o total.** Mostre o
intervalo — "21–37 de 37" —, nunca só o tamanho da página. A lista vem ordenada do estoque menor para
o maior, e cada linha traz `skuId` e `version` — ofereça **as duas formas de ajuste** ali mesmo, sem
navegar até o produto.

As duas não são variações de um mesmo controle e a tela deve deixar isso explícito, porque "some 3" e
"passe a valer 7" só coincidem quando a leitura de partida está correta:

| | Envia | Motivo padrão | Erro esperado |
|---|---|---|---|
| **Movimento** | `{ delta, reason }` | `RESTOCK` | `400` se ficaria negativo — o `detail` traz o disponível |
| **Contagem** | `{ absolute, version, reason }` | `INVENTORY_COUNT` | `409` se a versão envelheceu |

`delta: 0` é `422`; `absolute: 0` é válido e comum — prateleira vazia. No `409` não há número a
corrigir no formulário: a leitura inteira envelheceu, e a única saída é reler. Ofereça o recarregar
ali, em vez de deixar o operador adivinhar.

Guarde a `version` que o `PATCH` devolve: ela já vem **incrementada**, e uma segunda contagem no mesmo
SKU que reenvie a versão da listagem leva `409` garantido.

Calcule o número de páginas com o `lowStockPageSize` da resposta, e não com um `20` escrito no
cliente: repetida, a constante vira uma paginação silenciosamente errada no dia em que o servidor
mudar o tamanho da página. **Zere `lowStockPage` sempre que o limiar mudar** — baixar o limiar encolhe
o conjunto, e a página em que se estava passa a devolver vazio, que se lê como "não há mais alertas".

Só entram produtos **ativos e não removidos**: o alerta serve para avisar que a loja vai perder
venda, e produto fora da vitrine não perde venda.

**As janelas de faturamento são fixas e contam dias inteiros** a partir da meia-noite. `today` é de
hoje 00:00 em diante; `last7Days` cobre hoje e os seis anteriores. Não são janelas móveis de 24h.

> **Não coloque seletor de datas no dashboard.** Quem precisa recortar um período tem os filtros da
> listagem de pedidos. Duas telas com seletor seriam duas respostas possíveis para a mesma pergunta.

> ### ⚠️ `revenue` é valor de pedido, não dinheiro recebido
>
> Somam-se apenas `PAID`, `SHIPPED` e `DELIVERED`. Como cancelar um pedido pago não estorna nada, um
> cancelamento desses **tira o valor daqui sem tirar o dinheiro da Stripe**.
>
> Rotule com honestidade: "faturamento reconhecido" descreve o número; "recebido" não.

---

## 8. Histórico de alterações

Uma linha por alteração administrativa: quem fez, o quê, em qual registro e quando. É a tela que
responde *"por que o estoque está 7 se eu coloquei 10?"* e *"quem tirou este produto do ar?"* —
perguntas que nenhuma coluna dos registros originais consegue responder, porque o banco guarda o
estado final e não quem o produziu.

### 8.1 `GET /api/v1/admin/audit` → `200 Page<AuditLogResponse>`

```ts
type AuditedEntity = 'PRODUCT' | 'PRODUCT_SKU' | 'COLLECTION' | 'ORDER'

type AuditAction =
  | 'CREATED'
  | 'UPDATED'
  | 'DELETED'
  | 'RESTORED'
  | 'STATUS_CHANGED'              // pedido
  | 'STOCK_ADJUSTED'              // SKU
  | 'PROMOTIONAL_PRICE_CHANGED'   // produto

type AuditLogResponse = {
  id: number
  actor: string                   // e-mail de quem estava logado, ou "system"
  entityType: AuditedEntity
  entityId: string                // string, mesmo quando o id é numérico — ver §8.3
  action: AuditAction
  previousValue: string | null    // preenchidos aos pares, só nas ações de mudança de campo
  newValue: string | null
  reason: StockChangeReason | null   // só em STOCK_ADJUSTED
  details: string | null          // texto livre de contexto
  createdAt: string               // ISO
}
```

Paginado, `size=20` por padrão, `createdAt desc`. Ordenação restrita a `id`, `createdAt`, `actor`,
`action`, `entityType` — qualquer outro campo em `?sort=` devolve `400` (§1.7).

### 8.2 Filtros

Todos opcionais e combináveis, como query params:

| Parâmetro | Tipo | Comportamento |
|---|---|---|
| `entityType` | `AuditedEntity` | |
| `entityId` | `string` | **casamento exato** — o histórico do produto 4 não arrasta o do 42 |
| `actor` | `string` | casa por trecho, sem diferenciar maiúsculas: `maria` acha `maria@atelier.com` |
| `action` | `AuditAction` | |
| `createdFrom` | `YYYY-MM-DD` | inclusivo |
| `createdTo` | `YYYY-MM-DD` | inclusivo — **o dia inteiro entra** |

```
GET /api/v1/admin/audit?entityType=PRODUCT&entityId=42
GET /api/v1/admin/audit?action=STOCK_ADJUSTED&createdFrom=2026-08-01
GET /api/v1/admin/audit?actor=maria
```

Intervalo invertido (`createdFrom > createdTo`) devolve `400` em vez de lista vazia — vazio seria
lido como "ninguém mexeu em nada nesse período".

### 8.3 O que precisa ser tratado na interface

> **Só leitura.** Não existe rota para criar, editar ou apagar uma linha, e a tabela é imutável no
> banco. Não construa formulário aqui — a garantia de que o rastro não pode ser reescrito é o motivo
> de a tabela existir.

> **`entityId` é `string`.** As quatro entidades de hoje usam id numérico, mas a coluna guarda texto
> para caber o `UUID` de usuário quando promover alguém a `ADMIN` virar rota. Compare como string e
> não converta para `number` — `String(product.id)` ao montar o filtro.

> **`previousValue`/`newValue` são `null` nas ações que não mudam um campo.** `CREATED`, `UPDATED`,
> `DELETED` e `RESTORED` deixam os dois vazios; a informação está no próprio registro. Uma linha do
> tipo "de X para Y" só existe em `STATUS_CHANGED`, `STOCK_ADJUSTED` e
> `PROMOTIONAL_PRICE_CHANGED`.

> **`null` em `PROMOTIONAL_PRICE_CHANGED` significa ausência de promoção**, não valor desconhecido.
> `null → "149.90"` é uma promoção criada; `"149.90" → null` é uma promoção retirada. Renderize as
> duas como frases diferentes.

> **Uma edição de produto com mudança de preço gera duas linhas** — `UPDATED` e
> `PROMOTIONAL_PRICE_CHANGED`, com o mesmo `actor` e `createdAt` praticamente igual. São perguntas
> diferentes: "alguém salvou o formulário" e "a promoção mudou". Se a tela precisar mostrá-las como
> um evento só, agrupe por `entityId` + `createdAt` no cliente.

> **`details` é texto livre**, e o formato varia por ação: `"SKU VD-001-M"` no ajuste de estoque,
> `"3 products detached"` na exclusão de coleção, `"2 products still deleted"` na restauração. Exiba
> como está; não tente parsear.

> ### ⚠️ O que **não** aparece aqui
>
> - **A transição para `PAID` vinda do webhook do Stripe.** A tabela registra ação de operador; o
>   pagamento tem registro autoritativo do lado do Stripe. Na prática, o histórico de um pedido
>   começa na primeira ação manual — se a tela sugerir "linha do tempo completa do pedido", vai
>   mentir.
> - **Os campos alterados num `UPDATED`.** A linha diz que houve edição, não o diff. Guardar a árvore
>   inteira do produto seria versionamento, que é outro recurso.
> - **Ações de cliente.** Checkout, carrinho, cadastro e login não entram — o nome da tabela é
>   `admin_audit_log` e o escopo é o painel.

### 8.4 Onde usar

Duas telas, e a segunda vale mais que a primeira:

1. **`/admin/audit`** — a lista global, com os filtros acima. Útil para "o que aconteceu hoje".
2. **Uma aba na tela de edição** de produto e de coleção, já filtrada por `entityType` e `entityId`.
   É onde a pergunta realmente nasce: o operador está olhando o registro e quer saber o que houve
   com ele. Uma chamada, um filtro fixo, nenhum estado novo.

Para a tabela de SKUs, o filtro é `entityType=PRODUCT_SKU&entityId={skuId}` — o histórico de estoque
daquele SKU, com o motivo de cada movimento.

---

## 9. Upload de imagens

### 9.1 `POST /api/v1/admin/uploads` → `201`

`multipart/form-data` com dois campos:

| Campo | |
|---|---|
| `files` | um ou mais arquivos |
| `folder` | `products` ou `collections`; padrão `general`. Use valores fixos |

Resposta: `{ "urls": ["https://res.cloudinary.com/..."] }`

### 9.2 O fluxo é em duas etapas, e a interface precisa dizer isso

**Subir a imagem não salva o produto.** A URL retornada tem que ir para o campo correspondente do
formulário e só é persistida no `POST`/`PUT` seguinte. Um admin que suba a imagem e feche a aba
perde a associação — o arquivo fica no Cloudinary, órfão.

Deixe o estado visível: "imagem enviada, salve o produto para aplicar".

### 9.3 Restrições

- Formatos: `image/jpeg`, `image/png`, `image/webp`. **A assinatura binária é conferida**, não só o
  `Content-Type` — renomear um `.pdf` para `.jpg` é rejeitado com `415`.
- 5MB por arquivo, 20MB por requisição → excesso devolve `413`.

> ### ⚠️ Envie **um arquivo por requisição**
>
> O endpoint aceita lote, mas não é transacional: se o terceiro arquivo falhar, os dois primeiros já
> subiram ao Cloudinary e a resposta volta como erro, **sem URL nenhuma**. As imagens ficam órfãs e
> irrecuperáveis pela interface.
>
> Uma requisição por arquivo torna cada falha isolada e recuperável, e ainda dá progresso por item.

---

## 10. Sugestão de arquitetura de telas

Não é prescritivo — é o mínimo que cobre os fluxos acima.

```
/admin                      → dashboard (§7)
/admin/products             → listagem + filtros + ordenação (§3.2)
/admin/products/new         → formulário de criação (§3.3)
/admin/products/[id]        → formulário de edição + tabela de SKUs com ajuste de estoque (§3.3, §4.4)
/admin/collections          → listagem (§5.2)
/admin/collections/new      → formulário
/admin/collections/[id]     → formulário
/admin/orders               → listagem + busca + faixa de datas (§6.2)
/admin/orders/[id]          → detalhe + mudança de status (§6.3, §6.4)
/admin/audit                → histórico global + filtros (§8)
```

A aba de histórico dentro de `/admin/products/[id]` e `/admin/collections/[id]` vale mais que a lista
global: é onde a pergunta nasce (§8.4).

### 10.1 Invalidação de cache no cliente

O servidor invalida o cache dele sozinho a cada escrita. No cliente, invalide as queries de catálogo
**e** de admin depois de cada mutação — o painel e a vitrine leem dados sobrepostos e ficam
dessincronizados sem isso.

Especificamente: o `PATCH` de estoque muda a disponibilidade que a página pública do produto mostra.

### 10.2 Confirmações destrutivas

Três operações precisam de confirmação explícita, e cada uma por um motivo diferente:

| Operação | O que o diálogo precisa dizer |
|---|---|
| `DELETE /admin/collections/{id}?cascadeProducts=true` | quantos produtos serão apagados |
| `PATCH .../status?newStatus=CANCELLED` num pedido `PAID` | **que o pagamento não será estornado** |
| `DELETE /admin/products/{id}` | que é reversível — reduz o medo, e é verdade |

### 10.3 Ordem de implementação sugerida

1. **Bootstrap:** cliente axios com CSRF, `GET /auth/me`, guarda de rota por `role`.
2. **Pedidos** — listagem e detalhe. É só leitura, valida a autenticação e a paginação, e é a tela
   que o negócio usa todo dia.
3. **Mudança de status**, com o mapa de transições. Primeira escrita: valida o CSRF de ponta a ponta.
4. **Dashboard.** Leitura pura, agrega o que já foi aprendido.
5. **Produtos: listagem.** Filtros e ordenação.
6. **Produtos: formulário.** O maior pedaço. Comece pelo round-trip `GET → PUT` sem alteração e só
   depois adicione edição.
7. **Estoque**, na tabela de SKUs.
8. **Coleções**, incluindo o `409` de nome como ação de restauração.
9. **Upload**, integrado aos dois formulários.
10. **Histórico**, começando pela aba dentro das telas de edição — é uma chamada com filtro fixo, e
    entrega mais que a lista global.

---

## 11. Checklist de verificação

Antes de considerar o painel pronto:

- [ ] `xsrfCookieName: '__Host-XSRF-TOKEN'` configurado — sem isso, **toda** escrita dá `403`
- [ ] Rewrite do Next ativo em **dev e produção**; nenhuma URL absoluta para o backend
- [ ] Interceptor **não** renova sessão em `403`
- [ ] `GET → PUT` sem alteração devolve `200` para um produto com duas cores
- [ ] Nenhum `stockQuantity` sai no payload de SKU com `id`
- [ ] Todos os `id` de cor e SKU preservados no estado do formulário
- [ ] Ordenação limitada aos campos da whitelist (§1.7)
- [ ] Seletor de status oferece só as transições válidas para o status atual
- [ ] Cancelamento de pedido `PAID` avisa sobre o estorno
- [ ] Exclusão de coleção com cascata mostra a contagem de produtos
- [ ] `409` de nome de coleção oferece restaurar, com o id extraído do `detail`
- [ ] Dashboard mostra "N de M" no estoque baixo
- [ ] Upload envia um arquivo por requisição
- [ ] Produtos com `deletedAt` na listagem oferecem "restaurar", não "editar"
- [ ] `entityId` do histórico tratado como string, nunca convertido para número
- [ ] Histórico não oferece nenhuma ação de escrita

---

## 12. Defeitos conhecidos do backend

Comportamentos que a interface vai encontrar e que **não** são erro seu.

| # | O que acontece | Impacto no front |
|---|---|---|
| BUG-05 | Cancelar pedido `PAID` não estorna | Avisar no diálogo de confirmação |
| BUG-06 | Cancelar pedido pendente deixa o PaymentIntent aberto na Stripe | Nada a fazer no cliente |
| BUG-11 | Upload em lote não faz rollback parcial | Enviar um arquivo por requisição |
| BUG-12 | Imagens nunca são apagadas do Cloudinary | Trocar a imagem de um produto deixa a antiga órfã |
| ~~BUG-13~~ | ~~Não existe rota para criar o primeiro admin~~ | **Corrigido.** `admin@tsm-atelier.com` vem de migration com papel `ADMIN` e e-mail já verificado. Em desenvolvimento a senha é `senha123`; em produção vem de `ADMIN_PASSWORD_HASH` |
| BUG-15 | Uma coleção removida que ocupava `HOME_MAIN` continua bloqueando a posição | `409` sem explicação ao tentar ocupar a posição |
| BUG-17 | Payload JSON parcial devolve `400 "Failed to read request"` em vez de `422` com `fields` | Enviar sempre o objeto completo; não confiar em `fields` para esse caso |
