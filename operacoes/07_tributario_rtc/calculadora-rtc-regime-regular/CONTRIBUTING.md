# Guia de Contribuição

Obrigado pelo interesse em contribuir com a **Calculadora de Tributos**!
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

- Versão da aplicação utilizada (campo `versao` no retorno da API ou `pom.xml`)
- Endpoint afetado e payload de entrada utilizado
- Comportamento observado vs. comportamento esperado
- Logs de erro relevantes
- Versão do Java e sistema operacional

### Sugestão de Melhoria

Ao sugerir uma melhoria, inclua:

- Descrição clara do problema ou necessidade
- Referência normativa aplicável (artigo de lei, nota técnica, portaria, etc.)
- Proposta de solução ou comportamento esperado
- Impacto esperado sobre outros endpoints ou funcionalidades

---

## 3. Pull Requests

**Este repositório não aceita Pull Requests abertos pelo público em geral no GitHub.**

Eventuais Pull Requests abertos por usuários externos serão encerrados sem mesclagem.

---

## 4. Padrões de Código

- **Linguagem dos identificadores:** Português sem acentuação (ex: `calcularAliquota`, `buscarNcm`)
- **Estilo:** convenções padrão Java (camelCase para métodos/variáveis, PascalCase para classes)
- **JavaDoc:** obrigatório em classes de domínio, serviços e controllers
- **Logs:** usar `SLF4J` com níveis adequados (`DEBUG`, `INFO`, `WARN`, `ERROR`). Proibido `System.out`
- **Testes:** testes unitários obrigatórios; testes de integração quando aplicável

---

## 5. Código de Conduta

Ao participar deste projeto, você concorda em seguir nosso [Código de Conduta](CODE_OF_CONDUCT.md).