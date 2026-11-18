# GUIA DE INFRAESTRUTURA & CONTAS DE E-MAIL CORPORATIVO
### THIAGO ROCHA DUARTE - CONSULTORIA E SERVICOS DE TI LTDA (TRD Consultoria)
**CNPJ:** 43.065.207/0001-57 | **Domínio Oficial:** `trdconsultoria.com.br`

---

## 1. Visão Geral da Infraestrutura de Correio Eletrônico
A arquitetura de e-mails corporativos da TRD Consultoria foi provisionada sob o domínio próprio `trdconsultoria.com.br`, hospedada nos servidores de alta disponibilidade do **Zoho Workplace / Zoho Mail**, com apontamentos DNS gerenciados diretamente no **Registro.br (NIC.br)** e tripla blindagem de autenticação (**MX, SPF e DKIM**).

* **Provedor:** Zoho Mail (Organização Corporativa TRD Consultoria)
* **Webmail de Acesso:** [https://mail.zoho.com](https://mail.zoho.com)
* **Console de Administração:** [https://mailadmin.zoho.com](https://mailadmin.zoho.com)
* **Aplicativo Desktop Oficial:** Zoho Mail Desktop para Windows (64-bit)
* **Aplicativo Mobile:** Zoho Mail para Android e iOS
* **Política de Segurança:** As credenciais de acesso são estritamente sigilosas e mantidas sob gestão exclusiva do Sócio Administrador em cofre de senhas cifrado (Bitwarden), com Autenticação de Dois Fatores (2FA) mandatória. **Nenhuma senha ou hash sensível é versionado neste repositório.**

---

## 2. Relação Oficial de Caixas Postais e Finalidades

| E-mail Corporativo | Titular / Perfil | Finalidade Operacional | Política de Acesso |
| :--- | :--- | :--- | :--- |
| **`thiago.duarte@trdconsultoria.com.br`** | **Thiago Rocha Duarte**<br>*(Sócio Proprietário)* | **Direção Executiva & Arquitetura Sênior:** Contatos institucionais de alto nível, fechamento de propostas comerciais executivas, assinaturas de contratos via Gov.br, decisões de engenharia e relacionamento direto com diretores/CTOs de clientes. | Acesso pessoal e intransferível do fundador. |
| **`contato@trdconsultoria.com.br`** | **Atendimento Comercial & Parcerias** | **Caixa de Entrada Comercial & Triagem:** Recepção de orçamentos gerados pelo site oficial, solicitações de criação de sistemas sob medida (CRM/Hubs), contatos de agências parceiras e canal de entrada de dúvidas gerais. | Caixa operacional monitorada para triagem e qualificação de novos leads. |
| **`trdconsultoria@trdconsultoria.com.br`** | **Administrador da Organização** | **Gestão de Infraestrutura & Governança:** Conta raiz do console administrativo Zoho, recepção de avisos de segurança, faturamento de provedores, alertas de sistema e auditoria de contas. | Acesso administrativo exclusivo para gestão de contas e DNS. |

---

## 3. Mapeamento Técnico de DNS Homologado (Registro.br)

A integridade e a alta entregabilidade de e-mails foram homologadas perante os principais provedores mundiais (Google Workspace, Microsoft 365, Apple Mail) mediante os seguintes apontamentos oficiais:

### 3.1. Registros de Entrada (Mail Exchanger - MX)
Responsáveis pelo roteamento e entrega infalível das mensagens na caixa postal:
* **MX 1:** Host `@` *(em branco)* | Prioridade: `10` | Destino: `mx.zoho.com`
* **MX 2:** Host `@` *(em branco)* | Prioridade: `20` | Destino: `mx2.zoho.com`
* **MX 3:** Host `@` *(em branco)* | Prioridade: `50` | Destino: `mx3.zoho.com`

### 3.2. Mecanismo de Autenticação Anti-Spam (SPF)
Autoriza formalmente os servidores do Zoho a emitir e-mails pelo domínio `trdconsultoria.com.br`, eliminando risco de spoofing:
* **Tipo:** `TXT` | Host: `@` *(em branco)*
* **Valor:** `v=spf1 include:zohomail.com ~all`

### 3.3. Assinatura Criptográfica de Origem (DKIM)
Certifica cada e-mail emitido com chave pública de 2048-bits, atestando autenticidade e evitando o lixo eletrônico:
* **Tipo:** `TXT` | Host / Seletor: `zoho._domainkey`
* **Valor:**
  ```text
  v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCI2P63PR6rxD2y7z3DwXlv+oqO14nLfrPOJTQOgAQgzY4DVszVxDo6+0tcuFF562rGZKdV+p9bl2mnl9tpjlFqzAJ+wEHGe5lyb/mFPmBm7ltnEbWA5Fsv06Q72M1l1YQIszBrC6GRc7f6dcGZVRKtHiGrphwoHM12AoE2PLFtRQIDAQAB
  ```

---

## 4. Padrões de Atendimento e SLA de Comunicação

1. **Mensagens Comerciais Recebidas (`contato@`):**
   * Meta de primeiro retorno executivo: **Até 4 horas úteis**.
   * Emissão de laudo/proposta comercial preliminar: **Até 24 horas úteis** após alinhamento de escopo.
2. **Mensagens de Sustentação & SLA Crítico (Clientes Contratados):**
   * Incidentes P1/P2 devem ser abertos preferencialmente no canal de WhatsApp corporativo de sobreaviso (`+55 61 99952-6374`) para acionamento imediato, acompanhados de registro por e-mail para fins de registro probatório.
3. **Padrão Visual de Assinatura:**
   * Todas as respostas executivas devem utilizar o template padronizado em [assinatura_email.html](../06_identidade_visual/assinatura_email.html), garantindo conformidade com a identidade visual soberana da marca.
