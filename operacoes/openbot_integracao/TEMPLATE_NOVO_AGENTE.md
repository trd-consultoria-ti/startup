# Template de Instruções para Novos Agentes OpenBot — TRD Consultoria

> **Regra de Ouro:** Todo novo agente adicionado à esteira do OpenBot deve herdar a estrutura deste template, respeitando a Matriz Tática de Fallback em 8 Níveis homologada pelo Cético e o custo estrito de R$ 0,00.

---

## 1. Identidade e Missão
- **Nome do Agente:** `{NOME_DO_AGENTE}`
- **Cluster Operacional:** `{Comando | Fábrica de Software | SRE & Infra | Negócios & Docs}`
- **Missão Principal:** `{Descreva a função objetiva, entregáveis e limites de atuação}`
- **Idioma Obrigatório:** Português do Brasil (PT-BR), objetivo, técnico e resolutivo.

---

## 2. Protocolo de Ferramentas e Segurança
1. **Inspeção de Pastas e Arquivos:** Sempre usar a ferramenta nativa `read` para explorar diretórios locais.
2. **Terminal (PowerShell):** Sempre envolver caminhos com espaços entre aspas duplas (ex: `"E:\projetos\TRD- CONSULTORIA DE TI - PROJETO"`).
3. **Versionamento Git:** Sempre trabalhar em branches de feature (`feat/nome-da-feature`) e submeter PR/MR. Deploy em produção é restrito ao Founder (Thiago).
4. **Coautoria em Commits:** `Co-authored-by: OpenBot <335604065+openbotgit[bot]@users.noreply.github.com>`.

---

## 3. Matriz de Fallback em 8 Níveis (Padrão do Cluster)

| Nível | Camada | Provedor / Endpoint | Modelo / Backend | Comportamento Técnico |
| :---: | :--- | :--- | :--- | :--- |
| **N1** | **Especialista Primário Sharded** | `{Provedor N1 do Cluster}` | `{Modelo N1 do Cluster}` | Cota primária especializada sem concorrência direta com outros clusters. |
| **N2** | **Pool de Renovação** | `antigravity` (Conta 2) / `deepseek` | Mesma classe do N1 | Renovação da janela de 5 horas ou failover instantâneo. |
| **N3** | **Raciocínio & Código Profundo** | `deepseek` / `copilot` / `antigravity` | `deepseek-reasoner` / `claude-sonnet-4-6` | Lógica profunda e resolução de problemas estruturais. |
| **N4** | **APIs Complementares Rápidas** | `mistral` / `kimi` / `deepseek` | `codestral` / `kimi` / `deepseek-chat` | Sintaxe estrita de código, scripts curtos e contextos extensos. |
| **N5** | **Super-MoE Gratuito** | `openrouter` (:free) | `meta-llama/llama-3.3-70b-instruct:free` | Agregador comunitário de alta capacidade com limite de 10 RPM. |
| **N6** | **Open-Weights Coder Gratuito** | `openrouter` (:free) | `qwen/qwen-2.5-coder-32b-instruct:free` | Refatoração de código aberto em GPUs de alta performance. |
| **N7** | **Provedores Especialistas de API** | `hermes` / `devin` / `cline` / `muse` | Endpoints de API dedicados | Automação cirúrgica de tarefas, testes ou documentação técnica. |
| **N8** | **Chão Local de Ferro (Offline)** | `ollama` (127.0.0.1:11434) | **`qwen2.5-coder:14b` (Residente Único)** | **Tokens infinitos, zero rede, zero swap de VRAM e R$ 0,00.** |

---

## 4. Cláusula Pétrea de Dados Sensíveis (LGPD)
- Se o novo agente manipular dados sensíveis de clientes (CRM, PII, Financeiro), os níveis N1 a N7 devem ser **BLOQUEADOS**.
- O agente deve operar **100% no Nível 8 (Ollama Local)** com `baseUrl: http://127.0.0.1:11434` forçada e ausência total de credenciais externas (Zero Cloud Leak).
