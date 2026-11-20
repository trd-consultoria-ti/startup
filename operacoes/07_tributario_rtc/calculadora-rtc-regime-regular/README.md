# Calculadora de Tributos

## Descrição do Projeto

API REST que realiza o cálculo da CBS, do IBS e do Imposto Seletivo (Reforma Tributária do Consumo — LC 214/2025) nas operações do regime geral, consulta as tabelas oficiais usadas no cálculo e gera e valida os grupos de tributação dos documentos fiscais eletrônicos.

---

## Público-Alvo

- Empresas de tecnologia
- Desenvolvedores
- Contribuintes em geral

---

## Funcionalidades

Todos os caminhos começam com `http://localhost:8080/api/calculadora`. A lista completa, com esquemas e exemplos, está no Swagger (`http://localhost:8080/api/swagger-ui.html`).

| Funcionalidade | Endpoint |
|---|---|
| Cálculo de tributos (a operação inteira falha no primeiro erro) | `POST /regime-geral` |
| Cálculo com observabilidade (erros por item, sem interromper os demais) | `POST /observabilidade/regime-geral` |
| Descrição de um código de erro da observabilidade | `GET /observabilidade/erros?codigo=REG-011` |
| Base de cálculo unificada da NF-e/NFC-e | `POST /base-calculo/base-calculo-unificada` |
| Base de cálculo unificada da NFS-e | `POST /base-calculo/base-calculo-unificada-nfse` |
| Geração do XML a partir do resultado do cálculo | `POST /xml/generate?tipo=nfe` |
| Validação de XML (grupo ou nota) contra o XSD | `POST /xml/validate?tipo=nfe&subtipo=grupo` |
| CST e classificações tributárias (cClassTrib) | `GET /dados-abertos/situacoes-tributarias/cbs-ibs`, `GET /dados-abertos/classificacoes-tributarias/...` |
| Fundamentações legais | `GET /dados-abertos/fundamentacoes-legais` |
| NCM e NBS | `GET /dados-abertos/ncm`, `GET /dados-abertos/nbs` |
| Alíquotas de referência (União, UF e município) | `GET /dados-abertos/aliquota-uniao`, `/aliquota-uf`, `/aliquota-municipio` |

Exemplos de utilização da API estão em [docs/exemplos.md](docs/exemplos.md).

---

## Requisitos

### Requisitos do Sistema

- **Java**: versão 25
- **Maven**: não precisa ser instalado; o Maven Wrapper (`mvnw` / `mvnw.cmd`) baixa a versão necessária na primeira execução (requer internet)
- **Git**: para clonar o repositório
- **Sistema Operacional**: Windows, macOS ou Linux

### Dependências Principais

O projeto utiliza as seguintes tecnologias:

- **Spring Boot 4.1.2**: Framework principal da aplicação web
- **SQLite**: Banco de dados embarcado
- **Flyway**: Migração e versionamento do banco de dados
- **Springdoc OpenAPI 3.2.0**: Documentação interativa da API (especificação OpenAPI 3.1)

---

## Instruções de Instalação

### 1. Pré-requisitos

Certifique-se de ter instalado o Java 25 ou superior e verifique as versões:

```bash
# Verificar versão do Java
java -version
```

### 2. Clonar o Repositório

```bash
git clone https://github.com/ReceitaFederal/calculadora-rtc-regime-regular.git
cd calculadora-rtc-regime-regular
```

### 3. Gerar o Banco de Dados (obrigatório)

O perfil `offline` lê o banco `./calculadora/db/calculadora-pro.db`, definido por `application.db.filename` em `application-offline.yml`. O caminho é relativo à raiz do projeto e o banco é aberto em modo somente leitura.

> **Atenção:** a aplicação só funciona se o banco estiver gerado. Sem ele, o perfil offline não inicia.

Execute a partir da raiz do projeto.

**Linux/macOS:**

```bash
mkdir -p ./calculadora/db
./mvnw flyway:migrate -Dflyway.configFiles=./flyway/flyway-pro.conf --batch-mode --errors
```

**Windows:**

```bat
mkdir calculadora\db
mvnw.cmd flyway:migrate -Dflyway.configFiles=flyway\flyway-pro.conf --batch-mode --errors
```

Confirme que o arquivo foi criado com `ls -lah ./calculadora/db/calculadora-pro.db` (Linux/macOS) ou `dir calculadora\db\calculadora-pro.db` (Windows).

### 4. Compilar e Executar os Testes

Os testes exigem o banco gerado no passo 3, pois a maioria são testes de integração com o perfil `testes`.

Executar todos os testes com relatório de cobertura (JaCoCo):

**Linux/macOS:**

```bash
./mvnw clean test jacoco:report
```

**Windows:**

```bat
mvnw.cmd clean test jacoco:report
```

O relatório de cobertura fica em `target/site/jacoco/index.html`.

### 5. Executar a Aplicação com perfil Offline (spring-boot:run)

**Linux/macOS:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=offline
```

**Windows:**

```bat
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=offline"
```

A aplicação estará disponível em `http://localhost:8080/api`.

### 6. Gerar o JAR e Executar com java -jar (perfil offline)

**Linux/macOS:**

```bash
./mvnw clean package
java -Dspring.profiles.active=offline -jar target/api-regime-geral.jar
```

**Windows:**

```bat
mvnw.cmd clean package
java "-Dspring.profiles.active=offline" -jar target\api-regime-geral.jar
```

Execute o JAR a partir da raiz do projeto, para que `./calculadora/db` seja encontrado.

---

## Referências

- [Code of conduct](https://www.contributor-covenant.org)
- [MIT license](https://choosealicense.com/licenses/mit/)
- [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
- [Semantic Versioning](https://semver.org/lang/pt-BR/)

## Licença

Este projeto é distribuído sob a licença especificada no arquivo [LICENSE](LICENSE).