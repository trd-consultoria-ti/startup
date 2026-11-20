# ADR012 - Segurança e CORS

## Data

> 24/07/2026

## Status

> Aceito

## Contexto

A API é consumida pelo frontend da RTC Calculadora (Angular) por URL absoluta, em origem diferente.
O projeto inclui `spring-boot-starter-security`, mas a configuração atual não exige autenticação nas rotas.

## Decisão

**Cadeia de segurança**
- [WebSecurityConfig](../../src/main/java/br/gov/serpro/rtc/config/security/WebSecurityConfig.java) define um `SecurityFilterChain` para `/**` com:
  - sessão `STATELESS`;
  - CSRF desabilitado;
  - `formLogin`, `logout`, `httpBasic` e `anonymous` desabilitados;
  - `anyRequest().permitAll()`;
  - `frameOptions` `sameOrigin` (necessário para o Swagger em frame, ver ADR006);
  - CORS habilitado por `cors(withDefaults())`.
- [CalculadoraTributoApplication](../../src/main/java/br/gov/serpro/rtc/CalculadoraTributoApplication.java) exclui `UserDetailsServiceAutoConfiguration`, para que o Spring Boot não gere usuário padrão.

**CORS**
- [CorsConfig](../../src/main/java/br/gov/serpro/rtc/config/cors/CorsConfig.java) cria o bean a partir de [CorsProperties](../../src/main/java/br/gov/serpro/rtc/config/cors/CorsProperties.java), prefixo `app.cors`.
- Origens permitidas em `app.cors.allowed-origins` (variável `APP_CORS_ALLOWED_ORIGINS`, lista separada por vírgula) e padrões em `allowed-origin-patterns` (variável `APP_CORS_ALLOWED_ORIGIN_PATTERNS`).
- Métodos: `GET`, `POST`, `OPTIONS`. Cabeçalhos: `Content-Type`, `Authorization`. `allow-credentials: false`. `max-age: 3600s`.
- Não se usa `"*"` como origem, conforme o comentário em `application-offline.yml`.
- No perfil `offline`, a origem padrão é `http://localhost:4200`, usada no desenvolvimento local do frontend Angular.

## Consequências

Positivas:
- Origens são configuráveis por ambiente sem recompilar
- Sem cookies de sessão, não há risco de CSRF por sessão, e o CSRF está desabilitado coerentemente

Negativas:
- Com `anyRequest().permitAll()`, qualquer cliente que alcance a API consegue consultá-la; a proteção depende de controle de rede ou de gateway
- `Authorization` está na lista de cabeçalhos permitidos, mas nenhum mecanismo de autenticação está configurado no código analisado 

## Notas

- Introdução de `WebSecurityConfig`, `CorsConfig`, `CorsProperties` e da exclusão de `UserDetailsServiceAutoConfiguration`: 24/07/2026.
