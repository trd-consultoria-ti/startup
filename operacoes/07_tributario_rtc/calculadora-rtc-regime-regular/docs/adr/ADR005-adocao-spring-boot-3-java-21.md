# ADR005 - Adoção de Spring Boot 3 e Java 21

## Data

> 26/05/2026

## Status

> Substituído pela [ADR011](./ADR011-adocao-spring-boot-4-java-25.md)

## Contexto

O projeto RTC Calculadora é uma API de produção que processa múltiplas requisições concorrentes,
realiza cálculos fiscais com expressões e valores monetários, serializa e valida documentos XML e
acessa dados de referência em SQLite local.

A escolha da plataforma de execução impacta compatibilidade com bibliotecas, suporte a APIs
Jakarta EE, capacidade de concorrência eficiente e ciclo de suporte. Versões mais antigas de Java
e Spring Boot não oferecem virtual threads estáveis nem compatibilidade com os namespaces Jakarta
usados pelas dependências do projeto.

## Decisão

Usar Java 21 e Spring Boot 3 como plataforma de execução.

Virtual threads (Project Loom) são habilitadas via propriedade Spring Boot nos perfis
principal e de API pública, permitindo concorrência eficiente em fluxos I/O-bound. O perfil
offline usa configuração explícita de pool de threads.

Virtual threads são usadas diretamente no processamento paralelo por item.

## Consequências

Positivas:
- Stack atual alinhado ao ecossistema do projeto (Spring Boot 3.5.13, Java 21 LTS)
- Virtual threads disponíveis para melhorar escalabilidade de fluxos I/O-bound
- Compatibilidade com namespaces Jakarta EE usados pelas dependências
- Records e APIs recentes da linguagem disponíveis onde fazem sentido

Negativas:
- Requer JDK 21 no ambiente de build e execução
- Bibliotecas antigas precisam ser avaliadas quanto à compatibilidade com Java 21 e Jakarta
- Virtual threads exigem atenção a bloqueios e bibliotecas não preparadas

## Notas

Referências:
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
