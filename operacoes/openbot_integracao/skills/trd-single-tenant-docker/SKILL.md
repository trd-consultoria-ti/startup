---
name: trd-single-tenant-docker
description: Provisionamento, isolamento de recursos e ciclo de vida de instâncias single-tenant de clientes na TRD Consultoria. Cria subdomínios Nginx, containers Docker dedicados (Node.js API + Postgres isolado), volumes persistentes protegidos, portas dinâmicas e scripts individuais de backup diário com retenção de 7 dias.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "SRE / Provisionamento"
---

# TRD Single-Tenant Docker Architecture

Esta skill governa o isolamento de dados e instâncias dos clientes da TRD, assegurando que cada cliente possua seu próprio banco de dados e container de aplicação, eliminando riscos de vazamento entre tenants.

## 1. Estrutura Canônica de Provisionamento por Cliente
- **Subdomínio:** `<cliente>.trdti.com.br` ou `<cliente>.<projeto>.com.br`
- **Reverse Proxy:** Nginx com terminação SSL Let's Encrypt automática e redirecionamento HTTPS.
- **Database:** Container PostgreSQL 16 dedicado ou database/schema totalmente isolado com credenciais únicas e RLS (Row Level Security).
- **Application:** Container Node.js/Express executando a imagem Docker versionada com limites de memória (ex: `--memory 512m`) e CPU.

## 2. Script Padrão de Inicialização (`deploy_cliente.sh` / `.bat`)
```bash
docker network create trd-net-<cliente>
docker run -d --name trd-db-<cliente> --network trd-net-<cliente> -v trd-data-<cliente>:/var/lib/postgresql/data postgres:16-alpine
docker run -d --name trd-app-<cliente> --network trd-net-<cliente> -p <porta_unica>:3000 trd-sistema-generico:latest
```

## 3. Rotina de Despejo e Suspensão
- Suspensão por inadimplência: `docker stop trd-app-<cliente>` (preserva dados intactos no volume `trd-data-<cliente>`).
- Expurgo seguro (LGPD): dump criptografado entregue ao cliente, seguido de `docker rm -v` e destruição do volume.
