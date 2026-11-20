# Calculadora de Tributos - Simples Nacional

## Descrição do Projeto

API REST que realiza o cálculo da CBS e do IBS (Reforma Tributária do Consumo — LC 214/2025) nas operações do regime do simples nacional.

---

## Público-Alvo

- Empresas de tecnologia
- Desenvolvedores
- Contribuintes em geral

---

## Funcionalidades

Todos os caminhos começam com `http://localhost:8082/api`. A lista completa, com esquemas e exemplos, está no Swagger (`http://localhost:8082/api/swagger-ui.html`).

| Funcionalidade | Endpoint |
|---|---|
| Cálculo do Simples Nacional com observabilidade (erros por item, sem interromper os demais) | `POST /calculadora/observabilidade/simples-nacional` |
| Descrição de um código de erro `VSN-` | `GET /calculadora/observabilidade/simples-nacional/erros?codigo=VSN-001` |
| Atividades do Simples Nacional vigentes em uma data, com filtros por anexo, tipo de documento e cClassTrib | `GET /calculadora/simples-nacional/atividades?data=AAAA-MM-DD` |
| Consulta de uma atividade pelo código, vigente na data | `GET /calculadora/simples-nacional/atividades/{codigo}?data=AAAA-MM-DD` |
| Informações de versão da aplicação, da base de dados e do ambiente | `GET /versao` |
| Status da versão no perfil offline, comparado à instalação online | `GET /versao/status` (somente perfil `offline`) |


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
git clone https://github.com/ReceitaFederal/calculadora-rtc-simples-nacional.git
cd calculadora-rtc-simples-nacional
```

### 3. Gerar o Banco de Dados (obrigatório)

O perfil `offline` lê o banco `./calculadora/db/calculadora-pro.db`, definido por `application.db.filename` em `application-offline.yml`. O caminho é relativo à raiz do projeto e o banco é aberto em modo somente leitura.

> **Atenção:** use os scripts SQL para gerar o arquivo do Banco de Dados localizados no repositório https://github.com/ReceitaFederal/calculadora-rtc-regime-regular

> **Atenção:** a aplicação só funciona se o banco estiver gerado. Sem ele, o perfil offline não inicia.

Copie para a raiz deste projeto a pasta flyway/ presente no repositório https://github.com/ReceitaFederal/calculadora-rtc-regime-regular

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

A API estará disponível em `http://localhost:8082/api`; as métricas ficam disponíveis em `http://localhost:9103/metrics`.

### 6. Gerar o JAR e Executar com java -jar (perfil offline)

**Linux/macOS:**

```bash
./mvnw clean package
java -Dspring.profiles.active=offline -jar target/api-simples-nacional.jar
```

**Windows:**

```bat
mvnw.cmd clean package
java "-Dspring.profiles.active=offline" -jar target\api-simples-nacional.jar
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