---
name: trd-docker-single-tenant
description: Orquestração e governança de instâncias Docker Compose single-tenant para clientes da TRD Consultoria. Cobre configuração dinâmica de Nginx reverse proxy, emissão e renovação automática de certificados SSL Let's Encrypt via Certbot, limites de CPU e memória (cgroups), isolamento de redes e volumes de dados.
---

# TRD Docker Single-Tenant — Provisionamento & Isolamento de Clientes

## Arquitetura de Isolamento por Cliente
Cada cliente da TRD possui um container isolado da aplicação (`trd-sistema-generico`), banco de dados exclusivo ou schema isolado no PostgreSQL 16 e subdomínio próprio (`<cliente>.trdti.com.br`).

### 1. Especificação do Stack Docker Compose por Tenant
```yaml
services:
  app_<cliente>:
    image: trd-consultoria-ti/trd-sistema-generico:latest
    container_name: trd_app_<cliente>
    restart: always
    environment:
      - NODE_ENV=production
      - TENANT_ID=<cliente>
      - PORT=3000
      - DATABASE_URL=postgresql://tenant_<cliente>:sec_passwd@db:5432/db_<cliente>
    networks:
      - trd_internal_net
      - nginx_proxy_net
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          memory: 256M
```

### 2. Nginx Reverse Proxy Dinâmico & SSL
- Configuração automática do virtual host:
  `server_name <cliente>.trdti.com.br;`
  `proxy_pass http://trd_app_<cliente>:3000;`
- Emissão de certificado SSL:
  `certbot --nginx -d <cliente>.trdti.com.br --non-interactive --agree-tos -m suporte@trdti.com.br`
- HSTS, HTTP/2 e headers de segurança obrigatórios (`X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`).

### 3. Comandos de Suspensão e Desprovisionamento
- Suspensão por inadimplência (D+15):
  `docker stop trd_app_<cliente>` (mantendo volumes e banco íntegros).
- Redirecionamento de tráfego para página informativa institucional de suspensão temporária.
