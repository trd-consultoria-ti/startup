# ANEXO I — FICHA TÉCNICA DO MVP: SISTEMA STARTER (R$ 9.800)
### ESPECIFICAÇÃO DE ESCOPO FECHADO, REGRAS DE NEGÓCIO & TRAVA DE ESCOPO
**Integrado ao Contrato de Desenvolvimento de Software Sob Medida**  
**Protocolo do Projeto:** `TRD-PRJ-[XXXX]` | **Data de Emissão:** [DD/MM/AAAA]

---

> [!IMPORTANT]
> **A REGRA DA FICHA TÉCNICA IMUTÁVEL:**  
> Este documento delimita com precisão cirúrgica o que **ESTÁ INCLUSO** e o que **ESTÁ FORA DO ESCOPO** da Versão 1 (MVP) do Sistema Starter. Qualquer tela, botão, campo ou integração não listada expressamente nesta Ficha Técnica será automaticamente direcionada para o **Backlog de Fase 2 no Plano Continuidade (R$ 1.800/mês)** após o Go-Live.

---

## 1. Dados Cadastrais & Interlocutor Único (SPOC)

* **Empresa Cliente:** [Razão Social do Cliente]
* **CNPJ:** [00.000.000/0000-00]
* **Interlocutor Único Designado (SPOC):** [Nome do Gestor Autorizado]
* **WhatsApp do SPOC:** [ (00) 00000-0000 ] | **E-mail:** [ email@empresa.com.br ]
* *Nota Contratual:* Somente o SPOC acima possui poderes para homologar entregas, solicitar ajustes de bugs e receber comunicações técnicas.

---

## 2. As 4 Telas do Sistema Starter (Escopo Fechado)

### TELA 1 — Autenticação & Gestão de Usuários
* **Campos:** E-mail corporativo, Senha criptografada, Nome completo, Perfil de acesso (Administrador ou Operador).
* **Ações:** Login, Logout, Redefinição de senha via e-mail, Listagem de usuários internos (máximo 15 usuários simultâneos).
* **Não Incluso:** Autenticação por biometria, login com redes sociais ou integração com Active Directory/LDAP corporativo.

### TELA 2 — Cadastro e Consulta de Clientes
* **Campos:** Razão Social / Nome, CNPJ / CPF, Telefone/WhatsApp, E-mail, Endereço básico, Status (Ativo/Inativo), Observações textuais.
* **Ações:** Cadastrar novo cliente, Editar cadastro existente, Buscar por nome ou documento, Listagem paginada (20 por página).
* **Não Incluso:** Consulta automática na Receita Federal via API paga (salvo contratação de crédito de API à parte), geolocalização ou mapa interativo.

### TELA 3 — Entrada de Pedidos / Ordens de Serviço (OS)
* **Campos:** Cliente selecionado, Data de emissão, Lista de itens/serviços (com valor unitário e quantidade), Valor total calculado automaticamente, Forma de pagamento prevista, Observações operacionais.
* **Ações:** Criar novo pedido/OS, Alterar status do pedido (Pendente ➔ Em Execução ➔ Concluído ➔ Cancelado), Gerar comprovante/espelho em formato PDF padronizado para impressão/envio.
* **Não Incluso:** Módulo de cálculo complexo de frete com transportadoras (ex: Correios/Melhor Envio), tabelas de preços dinâmicas por estado ou cálculo automático de substituição tributária (ST).

### TELA 4 — Painel Financeiro Básico & Dashboard do Dono
* **Campos:** Resumo de contas a receber por status (A Vencer, Recebido, Em Atraso), Total faturado no mês corrente.
* **Ações:** Marcar recebimento manual de pedido, Filtro por data (Mês atual, Mês anterior), Exportação simples da listagem em CSV (Excel).
* **Não Incluso:** Conciliação bancária OFX automática, emissão de boletos com registro bancário automático via VAN bancária ou emissão direta de NF-e na SEFAZ (disponível no Hub Pro R$ 16.500).

---

## 3. Infraestrutura & Tecnologias Empregadas

* **Frontend:** Interface Web moderna em Angular / TypeScript, responsiva para acesso em navegadores de computadores e smartphones.
* **Backend:** APIs REST estruturadas em Java (Spring Boot) ou Node.js, com alto padrão de segurança e sanitização de dados.
* **Banco de Dados:** Banco de Dados Relacional dedicado (PostgreSQL) com backups automatizados pelo provedor de nuvem.
* **Hospedagem:** Provisionamento em nuvem otimizada (Railway / Supabase / DigitalOcean) com faturamento direto no cartão da CONTRATANTE.

---

## 4. Cronograma de Entrega (15 a 20 Dias Úteis)

```
[Dia 01 ao 04]  Alinhamento de wireframe e modelagem do banco SQL
[Dia 05 ao 12]  Codificação das 4 telas e lógica de permissões
[Dia 13 ao 16]  Provisionamento no Staging TRD e testes de carga
[Dia 17 ao 20]  Homologação assistida com o SPOC + Treinamento Loom (15 min) + Go-Live
```

---

## 5. Condições Financeiras Acordadas

* **Valor Total do Projeto:** **R$ 9.800,00 (nove mil e oitocentos reais)** líquidos.
* **Forma de Pagamento Selecionada:**
  - [ ] **Boleto Bancário 50 / 50:** R$ 4.900,00 na assinatura + R$ 4.900,00 na Homologação/Go-Live.
  - [ ] **Cartão de Crédito Corporativo:** [ __ ] parcelas via link seguro (com encargos operacionais repassados).
  - [ ] **À Vista com Bonificação:** Pagamento integral no ato da assinatura.

---

**De Acordo (Assinatura do SPOC / Representante Legal da CONTRATANTE):**  
Data: _____ / _____ / 202X  
Nome: __________________________________________________________________
