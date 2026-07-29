# Análise completa do projeto — bugs e melhorias

> Documento de estudo gerado a partir de uma varredura do código em `main` (29/07/2026).
> Cada item traz **onde está**, **o que acontece de errado na prática** e **como corrigir**.
>
> Os itens de 🔴 **P0** a 🟢 **P3** estão ordenados por impacto. Sugestão: resolva todos os P0
> antes de qualquer coisa, depois P1. P2/P3 podem entrar aos poucos.
>
> O fluxo de pedido/pagamento (`OrderService`, `CheckoutService`, webhook, scheduler) **já foi
> corrigido** numa etapa anterior — o que sobrou dele está na seção final "Dívida residual do
> fluxo de pedidos".

---

## Índice

- [🔴 P0 — Segurança e perda de dados](#p0--segurança-e-perda-de-dados)
- [🟠 P1 — Bugs funcionais](#p1--bugs-funcionais)
- [🟡 P2 — Contrato da API, validação e consistência](#p2--contrato-da-api-validação-e-consistência)
- [🔵 P2 — Performance e banco de dados](#p2--performance-e-banco-de-dados)
- [🟢 P3 — Qualidade, arquitetura e manutenção](#p3--qualidade-arquitetura-e-manutenção)
- [Dívida residual do fluxo de pedidos](#dívida-residual-do-fluxo-de-pedidos)
- [Ordem de execução sugerida](#ordem-de-execução-sugerida)
- [Conceitos para estudar](#conceitos-para-estudar)

---

## 🔴 P0 — Segurança e perda de dados

### 1. Endpoint público que expõe o catálogo inteiro sem paginação (e é código morto)

**Onde:** `domain/product/controller/v1/ProductCatalogController.java` → `@GetMapping("/n-plus-one")`
e `domain/product/service/ProductService.java` → `findAllWithNPlusOne()` (marcado no próprio código como
`"❌ VERSÃO ANTIGA — PROBLEMA N+1 (remover depois de testar)"`).

**O que acontece:** `/api/v1/catalog/**` está em `SecurityConstants.PUBLIC_ROUTES`. Então
qualquer pessoa na internet pode chamar `GET /api/v1/catalog/n-plus-one`, que faz `findAll()`
sem `Pageable` e depois dispara uma query por produto, por cor e por SKU. Com alguns milhares
de produtos isso é um **DoS de uma linha de curl**: esgota o pool de conexões e derruba a
aplicação para todos os outros usuários.

**Correção:** apagar o endpoint e o método. Ele existia só para comparar com a versão otimizada.

```java
// ProductCatalogController.java — remover o bloco inteiro
// @GetMapping("/n-plus-one") ...
// ProductService.java — remover findAllWithNPlusOne()
```

**Regra para levar adiante:** nenhum endpoint de listagem sem `Pageable`. Nunca. Nem "temporário".

---

### 2. Produtos despublicados e deletados são legíveis publicamente

**Onde:** `ProductService.findById(...)` e `ProductService.findBySlug(...)`.

**O que acontece:** a listagem filtra corretamente via `ProductSpecification` (`cb.isNull(root.get("deletedAt"))`),
mas a busca individual não filtra **nem `active`, nem `deletedAt`**. Como o `Product` — diferente de
`ProductColor` e `ProductSKU` — **não tem `@SQLRestriction("deleted_at IS NULL")`**, o Hibernate
devolve o registro normalmente.

Consequência prática: você cadastra a coleção de inverno com `active = false` para lançar dia 1º,
alguém itera `/api/v1/catalog/products/1..N` e vê tudo — preços, fotos, nomes — antes do lançamento.
E produtos "deletados" continuam acessíveis pelo id/slug para sempre.

**Correção — duas camadas:**

1. Colocar a restrição na entidade (resolve o soft delete de uma vez, em todas as queries):

```java
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity { ... }
```

2. Separar o caso de uso: o catálogo público busca por `findByIdAndActiveTrue`, e o admin usa um
   método/rota própria que enxerga inativos.

**Por que a camada 1 não basta:** `@SQLRestriction` resolve o *deletado*, não o *despublicado*.
`active = false` é regra de negócio e tem que ser filtro explícito do caso de uso público.

---

### 3. Nenhuma configuração de CORS

**Onde:** não existe. Não há `CorsConfigurationSource`, `WebMvcConfigurer#addCorsMappings`, nem
`@CrossOrigin` em nenhum ponto do projeto.

**O que acontece:** hoje, o front-end simplesmente **não consegue** consumir a API do navegador —
todo request cross-origin é bloqueado no preflight. Você vai descobrir isso na primeira integração,
e o risco real é a "correção" de pressa: `allowedOrigins("*")` junto com `allowCredentials(true)`.
Essa combinação é rejeitada pelo Spring, e a variação que "funciona" (refletir o `Origin` recebido)
transforma o cookie HttpOnly de autenticação em vetor de CSRF a partir de qualquer site.

**Correção:** origens explícitas, vindas de configuração.

```java
@Bean
CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);          // nunca "*" com credenciais
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

E no `SecurityConfig`: `.cors(cors -> cors.configurationSource(corsConfigurationSource))`.

**Atenção ao webhook:** `/api/v1/webhooks/stripe` não é chamado por navegador — não precisa de CORS
e não deve aceitar origem nenhuma.

---

### 4. CSRF desabilitado com autenticação por cookie

**Onde:** `security/SecurityConfig.java` → `.csrf(AbstractHttpConfigurer::disable)`.

**O que acontece:** desabilitar CSRF é correto para API *stateless com Bearer token* — o navegador
não anexa o header `Authorization` automaticamente. Mas este projeto emite o JWT **em cookie
HttpOnly**, e cookie o navegador anexa sozinho. Ou seja: um site malicioso pode montar um
`<form>` que faz `POST /api/v1/orders/checkout` e o request sai **autenticado** como a vítima.

**Correção — escolha uma das duas, não meia de cada:**

- **(a) Manter cookie + reativar CSRF:** `CookieCsrfTokenRepository.withHttpOnlyFalse()`, o front lê
  o cookie `XSRF-TOKEN` e reenvia no header `X-XSRF-TOKEN`. Isento apenas o webhook do Stripe.
- **(b) Manter CSRF off + `SameSite=Strict`** no cookie de autenticação. Mais simples, e cobre o
  caso do formulário cross-site. Verifique se atende ao seu fluxo (redirect de pagamento pode
  precisar de `Lax`).

Hoje o cookie é criado sem `SameSite` (veja o item 6) — o pior dos mundos: sem CSRF **e** sem
`SameSite`.

---

### 5. Deletar um usuário apaga o histórico de pedidos

**Onde:** `db/migration/V8__Create_orders_tables.sql` → `orders.user_id ... ON DELETE CASCADE`.

**O que acontece:** um `DELETE FROM users WHERE id = ?` (ou um `userRepository.delete(user)` num
futuro "excluir minha conta") apaga em cascata todos os pedidos daquele usuário — inclusive os
**PAGOS e ENTREGUES**. Isso destrói registro fiscal e contábil, e é irreversível.

**Correção:**

```sql
ALTER TABLE orders DROP CONSTRAINT fk_orders_user;
ALTER TABLE orders ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;
```

E adotar soft delete / anonimização para usuários (`deleted_at`, e-mail e nome anonimizados),
mantendo o pedido intacto. Pedido pago é documento, não dado de perfil.

---

### 6. Cookie de autenticação com `maxAge` de 3 segundos e sem `SameSite`

**Onde:** `domain/auth/controller/v1/AuthController.java`.

**O que acontece:** `maxAge(accessTokenExpiration / 1000)` — mas `accessTokenExpiration` **já está em
milissegundos** no `application.yaml` (`3600000`), e `ResponseCookie#maxAge(long)` espera **segundos**.
A divisão está sendo aplicada duas vezes: `3600000 / 1000 / 1000` na prática. Resultado: cookie com
`Max-Age=3` — o usuário é deslogado 3 segundos depois de entrar.

**Correção:**

```java
ResponseCookie.from("accessToken", token)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")                                   // ver item 4
    .path("/")
    .maxAge(Duration.ofMillis(accessTokenExpiration))     // sem divisão manual
    .build();
```

`Duration.ofMillis(...)` elimina a classe inteira de erro de unidade. Use `Duration` em toda
configuração de tempo — inclusive no `@Value`, que o Spring converte para `Duration` nativamente.

---

### 7. Enumeração de usuários nos endpoints de autenticação

**Onde:** `domain/auth/service/AuthService.java`.

**O que acontece:** as respostas diferenciam "usuário não existe" de "existe mas a senha está errada":

| Endpoint | Situação | Resposta hoje |
|---|---|---|
| `register` | e-mail já cadastrado | `EmailAlreadyExistsException` → 409 |
| `login` | e-mail não existe | `BadCredentialsException` → 401 |
| `login` | existe, não verificado | `EmailNotVerifiedException` → 403 |
| `resendVerificationEmail` | e-mail não existe | `UserNotFoundException` → 404 |

Com isso um atacante monta uma lista de clientes reais da loja só variando o e-mail. Combinado com
a **ausência de rate limiting** (item 8), dá para varrer bases vazadas inteiras.

**Correção:**

- `resendVerificationEmail`: responder **sempre** `202 Accepted` com a mesma mensagem, exista ou não
  a conta. Só envie o e-mail se existir.
- `login`: usar `BadCredentialsException` genérica para credencial inválida. Manter o 403 de
  "não verificado" **só depois de validar a senha** — aí a informação já não é gratuita.
- `register`: o 409 é difícil de evitar sem prejudicar a UX. O mitigante correto é rate limiting,
  não esconder o conflito.

---

### 8. Nenhum rate limiting em `/login`, `/register` e `/resend-verification`

**Onde:** ausente em todo o projeto.

**O que acontece:** brute force de senha ilimitado (senha mínima é de 6 caracteres sem exigência
de complexidade — item 20), e `resend-verification` vira uma máquina gratuita de mandar e-mail
para terceiros, queimando a reputação do seu domínio no Resend.

**Correção:** o Redis já está no projeto — use-o.

```java
// Esboço: filtro/interceptor por IP + e-mail, janela deslizante
String key = "rl:login:" + clientIp;
Long hits = redisTemplate.opsForValue().increment(key);
if (hits != null && hits == 1L) {
    redisTemplate.expire(key, Duration.ofMinutes(15));
}
if (hits != null && hits > 10) {
    throw new TooManyRequestsException();
}
```

Alternativa pronta: **Bucket4j** com backend Redis. Adicione também lockout progressivo da conta
após N falhas consecutivas.

---

### 9. Refresh tokens armazenados em texto puro no Redis

**Onde:** `AuthService` (persistência do refresh token) + `config/RedisConfig.java`
(sem senha configurada no `application.yaml`).

**O que acontece:** quem lê o Redis — dump, backup, `redis-cli` numa rede interna, instância sem
`requirepass` — obtém tokens válidos e se autentica como qualquer usuário. Refresh token é
credencial de longa duração; deve ser tratado como senha.

**Correção:**

- Guardar apenas `SHA-256(token)` como chave/valor e comparar o hash na validação. Não precisa de
  bcrypt aqui (o token já tem entropia alta), mas **precisa** ser hash.
- Definir `spring.data.redis.password` e habilitar TLS onde houver rede não confiável.
- Implementar **rotação**: cada refresh invalida o token anterior. Se um token já usado reaparecer,
  revogue toda a família de sessões daquele usuário — é sinal de roubo.

---

### 10. `RedisConfig` com desserialização polimórfica ampla

**Onde:** `config/RedisConfig.java` → `activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, ...)` com
`allowIfBaseType(Object.class)`.

**O que acontece:** `allowIfBaseType(Object.class)` libera **qualquer** classe a ser instanciada a
partir do JSON gravado no cache. Se um atacante conseguir escrever no Redis (item 9: sem senha),
ele controla o campo `@class` e ganha um gadget de desserialização → potencial RCE. É a mesma
família de falha do CVE-2017-4995 (Spring Security/Jackson).

**Correção:** restringir o `PolymorphicTypeValidator` aos seus próprios pacotes.

```java
BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType("com.tm.tsm_atelier.")
        .allowIfBaseType("java.util.")
        .build();
```

Melhor ainda: cachear **DTOs de resposta** (records simples) em vez de entidades JPA e dispensar o
default typing por completo.

---

### 11. Upload aceita qualquer arquivo, sem validação de tipo ou tamanho

**Onde:** `common/controller/v1/UploadController.java` +
`infrastructure/storage/CloudinaryStorageAdapter.java`.

**O que acontece:** não há checagem de content-type, de extensão nem de *magic bytes*. Um endpoint
autenticado de admin aceita `.svg` com `<script>` embutido (XSS armazenado servido do seu domínio
Cloudinary), `.html`, ou um arquivo gigante que consome banda e cota.

**Correção:**

```java
private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

if (!ALLOWED.contains(file.getContentType())) {
    throw new InvalidFileTypeException("Formato não suportado: " + file.getContentType());
}
```

Reforce com:
- Limite real de tamanho: `spring.servlet.multipart.max-file-size: 5MB` (hoje o default de 1MB pode
  estar cortando uploads legítimos sem mensagem clara).
- Validar o cabeçalho binário, não só o content-type declarado (o cliente mente).
- No Cloudinary, usar `resource_type: "image"` — ele rejeita não-imagens no lado dele.

---

### 12. Imagens órfãs: `deleteImage` nunca é chamado

**Onde:** `CloudinaryStorageAdapter.deleteImage(...)` existe e implementa `StoragePort`, mas um grep
no projeto mostra **zero chamadas**.

**O que acontece:** trocar a foto de capa de uma cor, deletar um produto ou uma coleção não remove
nada do Cloudinary. A cota cresce para sempre, e imagens de produtos "deletados" continuam públicas
por URL direta — o soft delete não esconde o asset.

**Correção:** apagar o arquivo antigo quando a URL é substituída ou o dono é removido. Mas
**cuidado com a ordem**: apagar dentro da transação e depois dar rollback deixa o banco apontando
para um arquivo que não existe mais. Faça no `AFTER_COMMIT`, com o mesmo padrão de evento já usado
em `OrderPaidEvent`:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onImageReplaced(ImageReplacedEvent event) {
    try {
        storagePort.deleteImage(event.publicId());
    } catch (RuntimeException e) {
        log.error("Falha ao remover imagem {} do storage", event.publicId(), e);
    }
}
```

---

## 🟠 P1 — Bugs funcionais

### 13. `User.isEnabled()` sempre retorna `true`

**Onde:** `domain/user/entity/User.java`.

**O que acontece:** o método de `UserDetails` está com retorno fixo. Efeitos:

- Não existe forma de banir/suspender um usuário — mesmo que você adicione uma flag `enabled` no
  banco, o Spring Security nunca vai olhar.
- O handler de `DisabledException` no `GlobalExceptionHandler` é **código morto**: nunca é lançado.

**Correção:** adicionar a coluna e refletir o estado real.

```java
@Column(nullable = false)
private boolean enabled = true;

@Override
public boolean isEnabled() {
    return enabled && deletedAt == null;
}
```

Vale revisar os outros métodos de `UserDetails` na mesma passada (`isAccountNonLocked` é o gancho
natural para o lockout do item 8).

---

### 14. `mergeColors` / `mergeSkus` comparam por identidade de referência

**Onde:** `ProductService.mergeColors(...)` e `mergeSkus(...)` — a comparação é `u == existing`.

**O que acontece:** `==` em objetos compara **endereço de memória**. Funciona por acidente enquanto
os dois lados vêm da mesma sessão do Hibernate, e quebra silenciosamente quando não vêm — por
exemplo se a entidade for lida do cache de segundo nível, vier destacada de outra transação, ou
depois de um `clear()`. Quando quebra, o merge trata uma cor existente como nova: cria duplicata
e/ou remove a original por `orphanRemoval` — **perdendo os SKUs e o estoque junto**.

Além disso os dois métodos são O(n·m): loop aninhado sobre cores × SKUs.

**Correção:** casar por identificador de negócio usando um `Map`.

```java
Map<Long, ProductColor> existingById = existing.getColors().stream()
        .filter(c -> c.getId() != null)
        .collect(Collectors.toMap(ProductColor::getId, Function.identity()));

for (ProductColorRequestDTO dto : request.colors()) {
    ProductColor target = dto.id() != null ? existingById.get(dto.id()) : null;
    if (target == null) {
        // nova cor
    } else {
        // atualiza a existente
    }
}
```

**Relacionado — item 15.**

---

### 15. Nenhuma entidade implementa `equals`/`hashCode`, mas há coleções `Set`

**Onde:** confirmado: nenhuma entidade do projeto define `equals`/`hashCode`.
E `Product.colors` é `Set<ProductColor>`, `ProductColor.skus` é `Set<ProductSKU>`,
`Product.careInstructions` e `ProductColor.galleryImages` são `Set<String>`.

**O que acontece:** sem `equals`/`hashCode`, `Set` usa identidade de referência
(`Object.equals`). Então `set.contains(mesmaEntidadeRecarregada)` retorna `false`, e
`set.remove(...)` não remove. É exatamente essa a raiz do bug do item 14.

**Correção — o padrão correto para entidades JPA** (compare por id, e nunca use o id no
`hashCode`, porque ele muda de `null` para um valor no `persist`):

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProductColor other)) return false;
    return id != null && id.equals(other.getId());
}

@Override
public int hashCode() {
    return getClass().hashCode();   // constante por tipo: seguro antes e depois do persist
}
```

⚠️ **Não** use `@EqualsAndHashCode` do Lombok em entidades: ele inclui todos os campos, o que
dispara carregamento de coleções LAZY e causa `StackOverflowError` em relações bidirecionais.

---

### 16. Slug regenerado a cada update, quebrando links já publicados

**Onde:** `ProductService.update(...)` chama `SlugUtils.generateSlug(...)` sempre.

**O que acontece:** corrigir um typo no nome do produto muda o slug, e toda URL já indexada pelo
Google, compartilhada no Instagram ou salva pelo cliente passa a dar 404. Perde-se SEO e tráfego.

**Correção:** o slug é imutável depois de publicado. Se precisar mudar, mantenha o antigo como
alias com redirect 301.

```java
if (existing.getSlug() == null) {                 // só na primeira vez
    existing.setSlug(SlugUtils.generateSlug(request.name()));
}
```

---

### 17. `SlugUtils.generateSlug` pode gerar slug duplicado, vazio ou malformado

**Onde:** `common/utils/SlugUtils.java`.

**O que acontece, em três frentes:**

1. **Sem garantia de unicidade.** `slug` é `unique` no banco. Dois produtos "Vestido Longo"
   (um WOMEN, outro em outra coleção) produzem o mesmo slug → `DataIntegrityViolationException`
   → **500** para o admin, sem explicação.
2. **`trim()` no lugar errado.** É o último passo da cadeia, mas `\\s+` → `-` já rodou antes.
   `"  Vestido  "` vira `"-vestido-"`, e o `trim()` não remove hífen. Slug com hífen nas pontas.
3. **Pode virar string vazia.** Um nome só com caracteres não-latinos (`"日本語"`) resulta em `""`.

**Correção:**

```java
public static String generateSlug(String name) {
    String base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")           // remove acentos, todos, não só os listados
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");         // remove hífens das pontas — o trim() não faz isso
    return base.isEmpty() ? "produto" : base;
}
```

E, no service, garantir unicidade com sufixo (`vestido-longo-2`) ou tratar a violação de
constraint devolvendo **409** com mensagem clara.

**Bônus:** `SlugUtils` é anotado com `@Component` mas só tem métodos `static` e é sempre chamado
estaticamente — o bean é inútil. Remova o `@Component` e adicione um construtor privado (como já
foi feito corretamente em `ZipCodeUtils`).

---

### 18. `CollectionService.delete` apaga produtos em cascata

**Onde:** `domain/collection/service/CollectionService.java` → `delete(...)` faz hard delete, e
`Collection.products` está mapeado com `cascade = ALL, orphanRemoval = true`.

**O que acontece:** deletar a coleção "Verão 2025" apaga **fisicamente** todos os produtos dela.
E, por cascata, cores e SKUs. Os `order_items` guardam nome/preço desnormalizados (bom!), mas
`order_items.sku_id` fica pendurado ou nulo. Um clique de admin destrói o catálogo de uma estação.

**Correção:**

1. Soft delete, coerente com o resto do domínio: `@SQLDelete` + `@SQLRestriction` em `Collection`.
2. **Remover o `cascade = ALL` da relação com produtos.** Coleção e produto têm ciclos de vida
   independentes: deletar a coleção deve apenas desassociar (`collection_id = NULL`).
3. Bloquear a deleção se houver produtos ativos, exigindo que o admin os mova primeiro.

---

### 19. TOCTOU no limite de 5 endereços e no endereço padrão único

**Onde:** `domain/user/service/AddressService.java`.

**O que acontece:** dois padrões clássicos de *check-then-act* sem proteção:

- **Limite de 5:** o código conta os endereços e depois insere. Dois requests simultâneos leem
  "4", ambos passam, e o usuário fica com 6.
- **Padrão único:** ao marcar um endereço como padrão, o código desmarca os outros e marca este.
  Dois requests concorrentes deixam **dois** endereços `isDefault = true`. Aí `findByUserIdAndIsDefaultTrue`
  (se retornar `Optional`) estoura `IncorrectResultSizeDataAccessException` → 500 no checkout.

**Correção:** invariante no banco, não só no Java.

```sql
-- Um único endereço padrão por usuário (índice parcial do Postgres)
CREATE UNIQUE INDEX uk_addresses_user_default
    ON addresses (user_id) WHERE is_default = true;
```

Para o limite de 5, tranque a linha do usuário antes de contar (`SELECT ... FOR UPDATE` via
`@Lock(PESSIMISTIC_WRITE)`), serializando as inserções daquele usuário. Ou aceite a corrida e
valide com um `CHECK` por trigger — mas o lock é mais simples aqui.

---

### 20. Política de senha fraca

**Onde:** DTO de registro — apenas `@Size(min = 6, max = 30)`.

**O que acontece:** `"123456"` é senha válida. Sem rate limiting (item 8), a conta cai em segundos.
O `max = 30` também é arbitrário e desnecessário — o BCrypt trunca em 72 bytes, não em 30.

**Correção:** mínimo de 8 (idealmente 12), sem exigir tabela de símbolos (o NIST desaconselha
regras de composição, que empurram o usuário para `Senha1!`). Prefira:

- `@Size(min = 8, max = 72)`
- Bloquear as senhas mais comuns por lista.
- `@Pattern` só se for requisito de negócio.

---

### 21. `findBySkuCode` ignora `deletedAt`

**Onde:** `ProductService.findBySkuCode(...)`.

**O que acontece:** `ProductSKU` tem `@SQLRestriction("deleted_at IS NULL")`, o que protege as
queries derivadas — mas se a busca usa JPQL/nativa customizada, a restrição pode ser contornada,
e um SKU descontinuado volta a aparecer para venda. Verifique também: como `sku_code` é `unique`
**sem cláusula parcial**, você **nunca poderá reutilizar** o código de um SKU deletado.

**Correção:** trocar por índice único parcial, coerente com o soft delete:

```sql
DROP INDEX IF EXISTS product_skus_sku_code_key;
CREATE UNIQUE INDEX uk_product_skus_sku_code
    ON product_skus (sku_code) WHERE deleted_at IS NULL;
```

---

### 22. `users.email` é case-sensitive

**Onde:** migration `V1`, e ausência de normalização no `AuthService`.

**O que acontece:** `Thierry@email.com` e `thierry@email.com` são duas contas diferentes. O usuário
se cadastra com maiúscula, tenta logar com minúscula e recebe "credenciais inválidas". Ou pior:
cadastra a segunda conta sem perceber e "perde" o histórico de pedidos.

**Correção:** normalizar na entrada (`email.trim().toLowerCase(Locale.ROOT)`) **e** garantir no banco:

```sql
CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));
```

Normalizar só no Java não protege contra registros antigos nem contra outro caminho de escrita.

---

## 🟡 P2 — Contrato da API, validação e consistência

### 23. `IllegalArgumentException` de regra de negócio virando 500

**Onde:** `ProductService.validateCategoryForAudience(...)`, `validateFabricComposition(...)`,
e `OrderService.getOrderDetails(...)` (`throw new IllegalArgumentException("Access denied")`).

**O que acontece:** o `GlobalExceptionHandler` não trata `IllegalArgumentException`, então ela cai
no handler genérico → **500 Internal Server Error**. Três problemas: o cliente não sabe que o erro
foi dele, o monitoramento enche de falso-positivo, e o "Access denied" deveria ser **403**
(hoje um usuário tentando ver o pedido de outro recebe um 500 que parece bug da API).

**Correção:** exceções de domínio específicas, mapeadas no handler.

| Exceção | Status | Caso |
|---|---|---|
| `InvalidCategoryForAudienceException` | 422 | categoria incompatível com o público |
| `InvalidFabricCompositionException` | 422 | percentuais não somam 100 |
| `AccessDeniedException` (do Spring) | 403 | pedido de outro usuário |

Adicione também um handler de fallback para `IllegalArgumentException` → 400, como rede de
segurança, mas sem depender dele.

---

### 24. Validações de DTO que não batem com o banco → 409 em vez de 422

**Onde:** DTOs de request de endereço, cor e SKU.

**O que acontece:** campos sem `@Pattern`/`@Size` compatível com a coluna:

| Campo | Coluna | Validação hoje | Resultado de um valor ruim |
|---|---|---|---|
| `zipCode` | `VARCHAR(8)` | nenhuma | `DataIntegrityViolation` → 409/500 |
| `state` | `CHAR(2)` | nenhuma | idem |
| `colorHex` | `VARCHAR(7)` | nenhuma | idem, e aceita `"azul"` |
| `skuCode` | `VARCHAR` unique | nenhuma | idem |

O cliente recebe um erro de banco onde deveria receber "campo X inválido".

**Correção:** validar na borda, sempre espelhando a coluna.

```java
@Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos")
String zipCode,

@Pattern(regexp = "[A-Z]{2}", message = "UF inválida")
String state,

@Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Cor deve estar no formato #RRGGBB")
String colorHex
```

Observação: `ZipCodeUtils.formatZipCode` remove os não-dígitos, mas roda **depois** da validação e
não impede um CEP de 15 dígitos de chegar ao banco.

---

### 25. `@Size(min = 3)` em nome e sobrenome rejeita nomes reais

**Onde:** DTOs de registro/atualização de usuário.

**O que acontece:** "Bo", "Al", "Ana Sá", sobrenomes como "Sá", "Li", "Ng" são recusados. É um bug
de exclusão: pessoas reais não conseguem se cadastrar na loja.

**Correção:** `@Size(min = 1, max = 100)` + `@NotBlank`. O `@NotBlank` já cobre o caso vazio, que
é o único que realmente importa.

---

### 26. `CollectionRequestDTO` sem `@NotNull` em campos obrigatórios

**Onde:** `displayPosition` e `targetAudience`.

**O que acontece:** `target_audience` é `NOT NULL` no banco. Se o cliente omitir o campo, o
`@Builder.Default` da entidade salva `WOMEN` **silenciosamente** — a coleção masculina inteira
nasce marcada como feminina e desaparece da vitrine correta. Falha silenciosa é pior que erro.

**Correção:** `@NotNull` nos dois. Deixe o default do builder apenas para testes/seed, nunca como
substituto de validação de entrada.

---

### 27. `getMe` reprocessa o JWT manualmente

**Onde:** `AuthController.getMe(...)`.

**O que acontece:** `/api/v1/auth/**` é público em `SecurityConstants.PUBLIC_ROUTES`, então o
`JwtAuthenticationFilter` não popula o `SecurityContext` de forma garantida para essa rota, e o
método reimplementa a extração do token. Duas lógicas de autenticação em paralelo é onde bugs de
segurança nascem: um dia elas divergem.

**Correção:** tirar `/auth/me` (e `/auth/logout`, `/auth/refresh` se aplicável) da lista pública e
usar a injeção padrão:

```java
@GetMapping("/me")
public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(UserResponseDTO.from(user));
}
```

Uma única fonte de verdade para "quem é o usuário".

---

### 28. Comentários `secure(true)` / "mudar para true no deploy" e configuração por ambiente

**Onde:** `AuthController` (comentários "Mudar para true quando for fazer deploy") e
`application.yaml`.

**O que acontece:** configuração de segurança controlada por comentário é configuração que vai
para produção errada. É questão de tempo.

**Correção:** perfis do Spring. `application.yaml` com o default seguro, `application-dev.yaml`
relaxando o que for necessário:

```yaml
app:
  cookie:
    secure: true      # default seguro; o perfil dev sobrescreve para false
    same-site: Strict
```

```java
@Value("${app.cookie.secure}") private boolean cookieSecure;
```

Assim o ambiente decide, não o desenvolvedor lembrando de editar antes do build.

---

## 🔵 P2 — Performance e banco de dados

### 29. Chaves estrangeiras sem índice

**Onde:** migrations `V1`, `V3`, `V7`.

**O que acontece:** **o Postgres não cria índice automaticamente em coluna de FK** (diferente do
MySQL/InnoDB). Sem eles, toda navegação faz *sequential scan*, e — pior — todo `DELETE` no lado
pai varre a tabela filha inteira para checar a FK.

Colunas afetadas: `addresses.user_id`, `product_colors.product_id`,
`product_skus.product_color_id`, `products.collection_id`.

**Correção:**

```sql
CREATE INDEX idx_addresses_user_id        ON addresses (user_id);
CREATE INDEX idx_product_colors_product_id ON product_colors (product_id);
CREATE INDEX idx_product_skus_color_id    ON product_skus (product_color_id);
CREATE INDEX idx_products_collection_id   ON products (collection_id);
```

Em produção com tabela grande, use `CREATE INDEX CONCURRENTLY` (fora de transação — o Flyway
precisa da migration marcada como não-transacional).

---

### 30. Tabelas de `@ElementCollection` sem chave primária nem índice

**Onde:** `product_fabric_compositions`, `product_care_instructions`, `product_gallery_images`.

**O que acontece:** sem índice na coluna de join, carregar as composições de um produto varre a
tabela toda. Sem PK, não há proteção contra linhas duplicadas e a replicação lógica do Postgres
não funciona bem nessas tabelas.

**Correção:** índice no join column, no mínimo:

```sql
CREATE INDEX idx_fabric_comp_product_id ON product_fabric_compositions (product_id);
CREATE INDEX idx_care_instr_product_id  ON product_care_instructions (product_id);
CREATE INDEX idx_gallery_color_id       ON product_gallery_images (product_color_id);
```

Para `careInstructions` e `galleryImages` (`Set<String>`), uma PK composta `(product_id, valor)` é
apropriada e já garante unicidade.

---

### 31. Sem `CHECK` de estoque e preço não negativos

**Onde:** migrations `V3` (`product_skus.stock_quantity`) e `V1`/`V3` (`products.price`).

**O que acontece:** a única barreira contra estoque negativo é o `if` em
`OrderService.createPendingOrder`. Qualquer outro caminho de escrita — um script de importação, um
ajuste manual de admin, um bug futuro — grava `-5` sem reclamar. E estoque negativo silencioso é
venda de produto inexistente.

**Correção:** o banco é a última linha de defesa. Use-a.

```sql
ALTER TABLE product_skus ADD CONSTRAINT chk_stock_non_negative
    CHECK (stock_quantity >= 0);
ALTER TABLE products ADD CONSTRAINT chk_price_non_negative
    CHECK (price >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_total_non_negative
    CHECK (total_amount >= 0);
```

Se algum registro atual violar, a migration falha — e isso é bom: você descobre agora.

---

### 32. `V6` ausente na sequência de migrations

**Onde:** `src/main/resources/db/migration/` — existem `V1`, `V3`, `V7`, `V8`, `V9`, `V10`.
Faltam `V2`, `V4`, `V5`, `V6`.

**O que acontece:** por si só o Flyway não se importa com buracos na numeração (ele ordena, não
exige contiguidade). Mas se alguma dessas migrations **já foi aplicada** no seu banco local e
depois removida do repositório, o `flyway_schema_history` tem um registro sem arquivo
correspondente → **`FlywayValidateException` no startup** de qualquer ambiente novo. E se elas
nunca existiram, o schema de um banco novo é diferente do seu banco local.

**Correção:** rodar `flyway info` (ou `SELECT version, description FROM flyway_schema_history ORDER BY installed_rank`)
e confirmar o que está registrado. Se houver registro órfão, ou recupere o arquivo, ou recrie o
banco de desenvolvimento a partir do zero — que é o único jeito de garantir que as migrations do
repositório produzem o schema esperado. Faça isso **antes** de aplicar a `V10`.

---

### 33. Coleções sem cache, produtos com cache

**Onde:** `CollectionService` não tem `@Cacheable`; `ProductService` tem.

**O que acontece:** as coleções são lidas em toda renderização de home/menu e mudam raramente —
é o caso de uso ideal para cache, e é justamente o que ficou de fora.

**Correção:** `@Cacheable("collections")` nas leituras e `@CacheEvict(allEntries = true)` nas
escritas. Mas antes resolva o item 10 (default typing do Redis) — e prefira cachear **DTOs**, não
entidades JPA: entidade serializada carrega proxies LAZY e estoura na desserialização.

---

### 34. `save` redundante e `saveAndFlush` como gambiarra

**Onde:** `CollectionService.create(...)` (dois `save` no mesmo objeto) e o `saveAndFlush` usado
para contornar a constraint de `HOME_FEATURED`.

**O que acontece:** dentro de uma transação, o Hibernate faz *dirty checking* — alterar a entidade
gerenciada já persiste no flush. O segundo `save` é ruído. O `saveAndFlush` força um flush no meio
da transação para "furar" a ordem de execução do Hibernate: funciona, mas é frágil e sinaliza que
a regra deveria estar no banco.

**Correção:** remover o `save` duplicado. Para a posição de destaque, expressar a invariante como
índice parcial:

```sql
CREATE UNIQUE INDEX uk_collections_home_featured
    ON collections (display_position)
    WHERE display_position = 'HOME_FEATURED' AND active = true;
```

Com a constraint no lugar, o `saveAndFlush` deixa de ser necessário e a regra passa a valer para
qualquer caminho de escrita.

---

### 35. `loadUserByUsername` consulta o banco em cada request

**Onde:** `security/JwtAuthenticationFilter.java`.

**O que acontece:** o filtro roda em toda requisição autenticada e faz um `SELECT` no usuário. Como
o JWT já carrega id e role, na maioria dos endpoints essa query é desnecessária — é um round-trip
por request, no caminho mais quente da aplicação.

**Correção:** duas opções, com trade-offs opostos:

- **Construir o `UserDetails` a partir das claims** do próprio token, sem tocar no banco. Mais
  rápido, porém uma mudança de role só vale no próximo login (aceitável com token de 1h).
- **Cachear o usuário no Redis** com TTL curto. Mantém a revogação quase imediata ao custo de uma
  dependência no caminho crítico.

Escolha pela política de revogação que você quer, e documente a decisão.

---

### 36. Linha morta no `JwtAuthenticationFilter`

**Onde:** `security/JwtAuthenticationFilter.java:61` → `String role = jwtService.extractRole(jwt);`
— a variável nunca é usada.

**O que acontece:** um parse extra de JWT por request, e — mais grave — indica que a **role do
token é ignorada**: a autoridade vem do `UserDetails` carregado do banco. Não é bug de segurança
(o banco é a fonte mais confiável), mas é confusão esperando para virar bug.

**Correção:** remover a linha. Se decidir usar as claims (item 35), aí sim ela passa a ser a fonte
das authorities — e o comentário deve deixar isso explícito.

---

## 🟢 P3 — Qualidade, arquitetura e manutenção

### 37. `ignoreFailures = true` nos testes

**Onde:** `build.gradle.kts` → `tasks.withType<Test> { ignoreFailures = true }`.

**O que acontece:** **teste que falha não quebra o build.** Isso anula o propósito da suíte: um
`./gradlew build` verde não significa nada. Toda a correção do fluxo de pedidos que você acabou de
fazer não tem rede de proteção contra regressão.

**Correção:** remover a linha. Se o problema é que os testes de integração precisam de Postgres,
a solução é **Testcontainers**, não desligar o sinal:

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
```

```java
@SpringBootTest
@Testcontainers
class TsmAtelierApplicationTests {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

Com `@ServiceConnection`, o Boot configura o datasource sozinho e o `contextLoads()` — que hoje
falha por falta de banco — passa a rodar em qualquer máquina e no CI.

**Este é o item de maior alavancagem do documento.** Sem ele, nenhuma das outras correções fica
protegida.

---

### 38. Ausência de testes para o fluxo crítico

**Onde:** existem testes de `AuthService` e do adapter de e-mail. Não há testes de
`OrderService`/`CheckoutService`, `ProductService`, `AddressService`.

**O que acontece:** o código que movimenta dinheiro e estoque é o único sem cobertura. Os bugs
corrigidos na etapa anterior (webhook respondendo 200 em falha, e-mail derrubando o status PAID,
scheduler cancelando pedido pago) são todos detectáveis por teste.

**Correção — comece por estes casos:**

| Cenário | O que verifica |
|---|---|
| `createPendingOrder` sem estoque | lança `OutOfStockException`, não reserva nada |
| `createPendingOrder` com SKU duplicado no carrinho | consolida a quantidade (bug já corrigido) |
| Gateway falha no `checkout` | estoque é devolvido, exceção original propaga |
| `handlePaymentSuccess` duas vezes | idempotente, um único e-mail |
| `handlePaymentSuccess` em pedido `CANCELLED` | loga ERROR, não marca PAID |
| Scheduler vs. webhook concorrentes | `OptimisticLockingFailureException` tratada, pedido fica PAID |

O último exige `@Version` funcionando — que já está em `Order`, mas **depende da `V10` ter sido
aplicada** (veja a nota de verificação ao final).

---

### 39. `AuthService.register` faz I/O externo dentro da transação

**Onde:** `domain/auth/service/AuthService.java` → `register(...)` é `@Transactional` e dentro dela
grava no Redis e dispara o e-mail de verificação.

**O que acontece:** é a **mesma classe de problema** que você acabou de corrigir no `OrderService`.
Duas diferenças atenuantes: o `ResendEmailAdapter` é `@Async("emailTaskExecutor")` e engole a
`ResendException`, então o e-mail não derruba a transação. Mas:

- O `@Async` dispara **antes do commit**. Se a transação der rollback depois, o usuário recebe um
  e-mail de verificação para uma conta que não existe — e o link quebra.
- A escrita no Redis não tem rollback. Falha no commit deixa o token de verificação órfão.

**Correção:** o mesmo padrão de `OrderPaidEvent`:

```java
eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFirstName()));
```

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onUserRegistered(UserRegisteredEvent event) { ... }
```

Com `AFTER_COMMIT`, o e-mail só sai se o usuário realmente existir. **Note que `@Async` não
resolve isso** — assíncrono não é o mesmo que pós-commit, e é fácil confundir os dois.

Aproveite para corrigir o comentário do código, que diz "Disparo assíncrono" como se isso já
garantisse o desacoplamento transacional.

---

### 40. Soft delete inconsistente entre entidades

**Onde:** `ProductColor` e `ProductSKU` têm `@SQLDelete` + `@SQLRestriction`. `Product` tem a
coluna `deletedAt` mas **nenhuma das duas anotações**. `Collection` e `User` não têm nem coluna.

**O que acontece:** o desenvolvedor (você, em três meses) não tem como saber se `delete()` numa
entidade apaga de verdade ou marca. Já produziu os itens 2 e 18 deste documento.

**Correção:** decidir uma política e aplicar em todo o domínio. Sugestão:
`Product`, `ProductColor`, `ProductSKU`, `Collection` e `User` são soft delete (têm valor
histórico); `Address` pode ser hard delete, desde que o pedido guarde o `ShippingAddress`
desnormalizado — e ele guarda. Documente isso num comentário na `BaseEntity` ou num
`ARCHITECTURE.md`.

---

### 41. Arquivos soltos na raiz do repositório

**Onde:** `generate_sql.py`, `update_postman.py`, `tsm-atelier-postman_collection.json`
(não rastreados no git, conforme o `git status`).

**O que acontece:** scripts utilitários e a coleção do Postman na raiz do projeto Java. Risco
concreto: coleções do Postman frequentemente contêm tokens e chaves reais. Se um `git add .`
distraído commitar isso, o segredo entra no histórico.

**Correção:** mover para `tools/` ou `docs/`, e adicionar ao `.gitignore` o que contiver segredo.
Se a coleção for útil ao time, versione uma versão **sanitizada** com variáveis de ambiente
(`{{baseUrl}}`, `{{accessToken}}`) em vez de valores.

---

### 42. Segredos no `application.yaml`

**Onde:** `application.yaml` — `stripe.api-key`, `stripe.webhook-secret`, `resend.api-key`,
credenciais do Cloudinary, segredo do JWT.

**O que acontece:** mesmo usando placeholders `${VAR}`, vale conferir se algum default literal
ficou no arquivo. Segredo em arquivo versionado é vazamento permanente: reescrever o histórico do
git é doloroso e a chave já foi exposta.

**Correção:** todos os segredos como `${STRIPE_API_KEY}` sem valor default, para que a aplicação
**falhe no startup** se a variável não existir — falha alta e imediata é melhor que rodar com
credencial errada. Em produção, use um gerenciador de segredos. Localmente, `.env` no `.gitignore`.

E, se algum segredo real já foi commitado: **rotacione a chave**. Removê-la do arquivo não a
invalida.

---

## Dívida residual do fluxo de pedidos

Itens conhecidos que ficaram deliberadamente de fora da correção anterior:

### 43. `getOrderDetails` devolve 500 em acesso negado

`OrderService.java:234` → `throw new IllegalArgumentException("Access denied")`. Já detalhado no
item 23. Deve ser `AccessDeniedException` → **403**.

### 44. `FIXED_SHIPPING_FEE` fixo em zero

`OrderService.java:54` → `new BigDecimal("0.00")`. Frete grátis hardcoded. Quando existir cálculo
de frete de verdade, ele precisa entrar **antes** do `PaymentIntent` (o valor cobrado é derivado do
total). Hoje não é bug — é uma decisão de negócio congelada em constante. Documente ou externalize
para configuração.

### 45. `updateOrderStatus` aceita qualquer transição

`OrderService.java:249` → `order.setStatus(newStatus)` sem validação. Um admin pode mover um pedido
de `CANCELLED` direto para `DELIVERED`, ou de `PENDING_PAYMENT` para `PAID` **sem pagamento** —
burlando o Stripe inteiro e sem restaurar/debitar estoque.

**Correção:** máquina de estados explícita.

```java
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
        OrderStatus.PAID,            Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED,         Set.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED,       Set.of(),
        OrderStatus.CANCELLED,       Set.of());

if (!ALLOWED_TRANSITIONS.get(order.getStatus()).contains(newStatus)) {
    throw new InvalidStatusTransitionException(order.getStatus(), newStatus);
}
```

Note que `PENDING_PAYMENT → PAID` **não** está na lista: essa transição pertence exclusivamente ao
webhook. E o cancelamento de um pedido `PAID` precisa disparar estorno + devolução de estoque —
não é só mudar o enum.

### 46. Cancelar pedido pago não devolve estoque nem estorna

Consequência do item 45. `cancelAndRestoreStock` só atua em `PENDING_PAYMENT`/`PAYMENT_FAILED`
(correto, por design). Falta o caminho de cancelamento pós-pagamento: `PaymentGatewayPort.refund(...)`,
devolução de estoque e e-mail ao cliente.

---

## ⚠️ Verificação pendente antes de tudo

A migration **`V10__Add_order_version_and_indexes.sql`** (criada na etapa anterior: coluna
`version`, índice único parcial em `payment_intent_id`, índices de `status/expires_at` e `user_id`)
**ainda não foi aplicada contra um banco real**. Como `spring.jpa.hibernate.ddl-auto` está em
`validate`, se ela não rodar a aplicação **não sobe** — o `@Version` em `Order` exige a coluna.

Antes de qualquer outra coisa:

1. Suba o Postgres local.
2. Confira o `flyway_schema_history` (item 32 — o buraco em `V2`/`V4`/`V5`/`V6`).
3. Aplique a `V10`.
4. Confirme que a aplicação inicia e que o `contextLoads()` passa.

Se o passo 2 revelar registro órfão, recriar o banco de desenvolvimento do zero é o caminho mais
rápido e o único que garante que o repositório produz o schema esperado.

---

## Ordem de execução sugerida

**Bloco 1 — desbloquear (faça hoje)**
1. Aplicar a `V10` e resolver a lacuna de migrations (item 32 + verificação pendente).
2. Remover `ignoreFailures = true` e adicionar Testcontainers (item 37). *Sem isso, nada abaixo fica protegido.*
3. Corrigir o `maxAge` do cookie (item 6) — bug de 1 linha que quebra o login por completo.

**Bloco 2 — segurança (esta semana)**
4. Remover o endpoint `/n-plus-one` (item 1).
5. Filtrar `active`/`deletedAt` na busca individual + `@SQLRestriction` em `Product` (item 2).
6. Configurar CORS por perfil (item 3) e decidir CSRF vs. `SameSite` (item 4).
7. `ON DELETE RESTRICT` em `orders.user_id` (item 5).
8. Rate limiting via Redis (item 8) e respostas uniformes em auth (item 7).
9. Hash dos refresh tokens + senha no Redis (item 9) e restringir o `PolymorphicTypeValidator` (item 10).
10. Validar content-type e tamanho no upload (item 11).

**Bloco 3 — bugs funcionais**
11. `equals`/`hashCode` nas entidades (item 15) e reescrever `mergeColors`/`mergeSkus` por id (item 14) — nessa ordem.
12. Congelar o slug no update (item 16) e corrigir o `SlugUtils` (item 17).
13. Remover o `cascade = ALL` de `Collection.products` + soft delete (item 18).
14. Índice parcial do endereço padrão e lock no limite de 5 (item 19).
15. `User.isEnabled()` real (item 13).
16. Normalizar e-mail + índice `LOWER(email)` (item 22).

**Bloco 4 — contrato e banco**
17. Exceções de domínio mapeadas para 4xx (itens 23, 43).
18. Validações de DTO alinhadas às colunas (itens 24, 25, 26).
19. Índices de FK e de element collections (itens 29, 30).
20. `CHECK` de estoque/preço não negativos (item 31).

**Bloco 5 — arquitetura**
21. `UserRegisteredEvent` com `AFTER_COMMIT` (item 39).
22. Máquina de estados de pedido (item 45) e fluxo de estorno (item 46).
23. Testes do fluxo crítico (item 38).
24. Limpeza de imagens no storage (item 12).
25. Política de soft delete uniforme e documentada (item 40).

---

## Conceitos para estudar

Os bugs deste documento se agrupam em poucos padrões. Vale estudar o padrão, não só a correção:

| Conceito | Itens relacionados | Por que importa aqui |
|---|---|---|
| **Fronteiras transacionais** | 39, e o `OrderService` já corrigido | I/O externo dentro de `@Transactional` prende conexões e desfaz efeitos que não podem ser desfeitos |
| **`@TransactionalEventListener` vs `@Async`** | 12, 39 | assíncrono ≠ pós-commit; confundir os dois gera e-mail de conta inexistente |
| **`equals`/`hashCode` em entidades JPA** | 14, 15 | `Set` + identidade de referência = duplicata silenciosa e perda de dados por `orphanRemoval` |
| **Invariante no banco vs. no código** | 19, 31, 34 | `if` em Java não sobrevive a concorrência nem a outro caminho de escrita |
| **TOCTOU / check-then-act** | 19 | ler-e-depois-escrever sem lock é sempre uma corrida |
| **Índices no Postgres** | 29, 30 | FK **não** ganha índice automático; índice parcial (`WHERE`) expressa invariantes condicionais |
| **Soft delete** | 2, 18, 21, 40 | `@SQLRestriction`, `unique` parcial e cascata precisam ser coerentes entre si |
| **Segurança de sessão em cookie** | 4, 6, 9 | `HttpOnly`, `Secure`, `SameSite`, CSRF e rotação de refresh token funcionam como conjunto |
| **Enumeração de usuários** | 7, 8 | resposta que diferencia "não existe" de "senha errada" vaza a base de clientes |
| **Desserialização polimórfica** | 10 | `DefaultTyping` amplo é gadget de RCE; restrinja o `PolymorphicTypeValidator` |
| **Máquina de estados de domínio** | 45, 46 | `setStatus` livre permite pedido entregue sem pagamento |
| **Testcontainers** | 37, 38 | teste de integração que precisa de infra não é motivo para desligar o build |

---

*Total: 46 itens. Os 23 do fluxo de pedido/pagamento analisados anteriormente já estão corrigidos,
exceto os itens 43 a 46 listados na seção de dívida residual.*
