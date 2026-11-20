---
name: trd-postgres-backup-dr
description: Procedimento determinístico de Backup e Disaster Recovery (DR) para PostgreSQL 16 na TRD Consultoria. Governa rotinas de pg_dump custom format (-Fc), validação de checksum SHA-256, criptografia simétrica com OpenSSL, teste automatizado de restore em container Docker efêmero e auditoria de RPO (< 24h) e RTO (< 30 min).
---

# PostgreSQL Backup & Disaster Recovery (DR) — TRD Consultoria

## Princípio Fundamental: "Backup Não Testado Não Existe"
Nenhum backup é considerado válido na TRD até ser restaurado e validado com sucesso em um container PostgreSQL efêmero.

### 1. Rotina de Dump Transacional
- Execução diária automatizada via `pg_dump`:
  ```bash
  pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -Fc -Z 6 -f /backups/db_${DB_NAME}_$(date +%Y%m%d_%H%M%S).dump
  ```
- Flags obrigatórias:
  - `-Fc`: Formato custom compactado, transacional e apto para paralelismo no restore (`pg_restore -j`).
  - `-Z 6`: Compressão calibrada para equilíbrio ótimo de tempo de CPU e tamanho de disco.
- Geração imediata de checksum de integridade:
  ```bash
  sha256sum backup.dump > backup.dump.sha256
  ```

### 2. Criptografia em Repouso
- Criptografia dos arquivos de dump com AES-256-CBC:
  ```bash
  openssl enc -aes-256-cbc -salt -pbkdf2 -in backup.dump -out backup.dump.enc -pass env:BACKUP_ENCRYPTION_KEY
  ```

### 3. Teste Automatizado de Restore (Container Efêmero)
1. Subir container Docker efêmero:
   ```bash
   docker run --rm -d --name pg_restore_test -e POSTGRES_PASSWORD=test postgres:16-alpine
   ```
2. Descriptografar e validar checksum `sha256sum -c`.
3. Executar o restore no container efêmero:
   ```bash
   docker exec -i pg_restore_test pg_restore -U postgres -d postgres -v < backup.dump
   ```
4. Executar query de sanidade (smoke query):
   ```sql
   SELECT count(*) FROM users;
   SELECT count(*) FROM contracts;
   ```
5. Medir o tempo total de restore (RTO real aferido).
6. Destruir o container efêmero imediatamente.

### 4. Métricas de Resiliência
- **RPO (Recovery Point Objective):** < 24 horas (máximo de dados que podem ser perdidos).
- **RTO (Recovery Time Objective):** < 30 minutos (tempo máximo para restaurar serviço completo).
- Notificação de falha: Se o teste de restore falhar, emitir alerta P1 imediato para o Ultron.
