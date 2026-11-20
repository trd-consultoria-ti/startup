# Política de Segurança

## Relato de Vulnerabilidades

Se você identificar uma vulnerabilidade de segurança neste projeto, pedimos que a reporte
de forma responsável, sem divulgar publicamente os detalhes antes que uma correção esteja disponível.

## Como Reportar

**Não abra uma Issue pública para relatar vulnerabilidades de segurança.**

Por favor, inclua as seguintes informações no relato:

- Descrição detalhada da vulnerabilidade e como ela pode ser explorada
- Endpoint ou componente afetado
- Passos para reprodução (proof of concept, se aplicável)
- Versão da aplicação onde a vulnerabilidade foi identificada
- Impacto estimado (confidencialidade, integridade, disponibilidade)
- Sugestão de correção, se houver

## Divulgação Coordenada

Após a correção ser disponibilizada em nova versão do projeto, a vulnerabilidade poderá ser
divulgada publicamente. Solicitamos que o pesquisador aguarde a publicação da correção antes
de divulgar detalhes técnicos.

## Escopo

Estão no escopo deste programa:

- Código-fonte publicado neste repositório
- API REST exposta pelo projeto (endpoints /calculadora/*)
- Lógica de cálculo tributário e seus componentes de domínio

Estão **fora** do escopo:

- Infraestrutura de produção e ambientes internos da RFB
- Dados reais de contribuintes
- Outros sistemas da Receita Federal não relacionados a este repositório
- Vulnerabilidades em dependências externas (reportar diretamente ao mantenedor da dependência)

## Versões com Suporte

Apenas a versão mais recente do projeto recebe correções de segurança.

## Agradecimento

A RFB reconhece e agradece os pesquisadores que contribuem responsavelmente para a segurança
deste projeto.

