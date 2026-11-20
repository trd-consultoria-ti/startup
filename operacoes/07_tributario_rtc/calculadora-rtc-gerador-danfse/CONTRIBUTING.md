# Guia de Contribuição

Obrigado pelo interesse em contribuir com o **DANFSe**!
Este guia descreve como você pode participar do projeto de forma organizada e efetiva.

---

## 1. Formas de Contribuição

Este projeto é mantido pela Receita Federal do Brasil (RFB) e pelo SERPRO.
**Pull Requests externos não são aceitos diretamente.** Contribuições externas devem ser
encaminhadas exclusivamente pelos canais oficiais listados abaixo:

| Canal                         | Tipo de Contribuição                                                                  |
|-------------------------------|--------------------------------------------------------------------------------------|
| **Issues do repositório**     | Relato de bugs, sugestões de melhoria, dúvidas técnicas                              |
| **Discussions do repositório**| Debate de propostas      |

> **Nota:** Sugestões e correções recebidas por Issues ou Discussions serão avaliadas pela
> equipe definida pela área de negócio da RFB responsável pelo projeto e, se aprovadas,
> incorporadas por meio dos processos internos de desenvolvimento.

---

## 2. Relatos sobre o código

### Bug Report

Ao relatar um bug, inclua:

- Versão da aplicação utilizada (tag Git ou commit)
- Endpoint afetado (ex.: `POST /api/danfse`) e parâmetros utilizados
- XML fictício ou redigido usado como entrada (sem dados pessoais ou de contribuintes reais)
- Comportamento observado vs. comportamento esperado
- Logs de erro relevantes
- Versão do Go e sistema operacional

### Sugestão de Melhoria

Ao sugerir uma melhoria, inclua:

- Descrição clara do problema ou necessidade
- Proposta de solução ou comportamento esperado
- Impacto esperado sobre endpoints ou funcionalidades existentes

---

## 3. Merge Requests

Antes de abrir uma proposta de alteração:

1. Verifique se já existe uma issue ou proposta relacionada.
2. Rode a suíte local: `task check` (`go fmt` e `go vet`) e `task test` (`go test -race -count=1 ./...`).
3. Mantenha a alteração focada e com mensagens de commit claras.

As propostas serão revisadas pelos mantenedores do repositório.

---

## 4. Padrões de Código

O projeto é escrito em **Go** e segue o estilo idiomático da linguagem.

- **Formatação**: use `gofmt`/`go fmt`. O CI falha se houver arquivos não formatados.
- **Análise estática**: `go vet` deve passar sem alertas.
- **Testes**: escreva testes para novas funcionalidades e correções; rode a suíte com `go test -race -count=1 ./...`.
- **Dados sensíveis**: nunca inclua credenciais, chaves, XMLs reais ou dados pessoais em código, commits ou issues. Use exemplos fictícios ou redigidos.
- **Erros**: trate erros explicitamente e evite ignorar ou "engolir" falhas.

---

## 5. Código de Conduta

Ao participar deste projeto, você concorda em seguir nosso [Código de Conduta](CODE_OF_CONDUCT.md).
