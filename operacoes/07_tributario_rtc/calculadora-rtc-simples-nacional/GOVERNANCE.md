# Governança do Projeto

## 1. Objetivo

Este documento define as regras de governança da **Calculadora de Tributos**,
projeto de código aberto mantido pela Receita Federal do Brasil (RFB) e desenvolvido em
parceria com o SERPRO. O objetivo é garantir a qualidade, rastreabilidade e sustentabilidade
do projeto, definindo papéis, responsabilidades e processos de decisão.

## 2. Papéis e Responsabilidades

| Perfil               | Responsabilidade                                                                                   | Instituição   |
|----------------------|----------------------------------------------------------------------------------------------------|---------------|
| **Administrador**    | Gestão de permissões, criação de repositórios, auditoria de acessos                                | RFB           |
| **Líder Técnico**    | Decisões estratégicas, aprovação de mudanças arquiteturais, alinhamento institucional              | RFB           |
| **Mantenedor**       | Revisão de código (code review), gestão de issues, garantia da qualidade e segurança               | RFB / SERPRO  |
| **Desenvolvedor**    | Desenvolvimento de funcionalidades, correção de bugs, escrita de testes                            | RFB / SERPRO  |
| **Leitor**           | Leitura do código-fonte, abertura de Issues e participação em Discussions                          | Externo       |

## 3. Versionamento

O projeto adota **Semantic Versioning (SemVer)** com a estrutura `MAJOR.MINOR.PATCH`:

- **MAJOR**: mudanças incompatíveis com versões anteriores da API
- **MINOR**: novas funcionalidades retrocompatíveis
- **PATCH**: correções de bugs retrocompatíveis

Todas as versões são registradas no [CHANGELOG.md](CHANGELOG.md).

## 4. Processo Decisório

### 4.1 Mudanças de Rotina

Mudanças de rotina incluem correções de bugs, atualizações de tabelas tributárias e melhorias
incrementais sem impacto arquitetural. O processo é:

1. Abertura de issue descrevendo a necessidade e referência normativa (quando aplicável)
2. Avaliação interna pela equipe da RFB
3. Se aprovada, incorporadas por meio dos processos internos de desenvolvimento
4. Validação automática por pipeline de integração contínua internos

### 4.2 Mudanças Arquiteturais

Mudanças que impactam a arquitetura, contratos de API ou estrutura do banco de dados exigem:

1. Elaboração de Architecture Decision Record (ADR) em `docs/`
2. Revisão e aprovação pelo Líder Técnico e Mantenedores
3. Validação de conformidade com as normas da Reforma Tributária sobre o Consumo
4. Registro no CHANGELOG.md e atualização do ROADMAP.md quando aplicável

### 4.3 Publicação de Novas Versões (Release)

O processo de publicação de novas versões segue:

1. Atualização do `pom.xml` com a nova versão SemVer
2. Atualização do `CHANGELOG.md` com todas as alterações da versão
3. Criação de tag Git com o número da versão (`vMAJOR.MINOR.PATCH`)
4. Publicação de release no repositório com as notas de versão

## 5. Canais de Comunicação

- **Issues**: para relato de bugs e sugestões de melhoria técnica, examinadas pela equipe definida pela área de negócio da RFB
- **Discussions**: para debate de propostas e esclarecimentos, examinadas pela equipe definida pela área de negócio da RFB