# ADR002 - Padrão JAXB Thread-Safe

## Data

> 26/05/2026

## Status

> Aceito

## Contexto

A API RTC Calculadora processa requisições concorrentes em Java 25 e gera documentos XML para
diferentes modelos fiscais (NFe, NFCe, CTe, BPe, NF3e). A serialização XML usa Jakarta XML
Binding.

`JAXBContext` é thread-safe, portanto pode ser compartilhado entre threads. `Marshaller` e `Unmarshaller`
não são thread-safe: compartilhar uma mesma instância pode causar race conditions e comportamento
não determinístico, incluindo `ArrayIndexOutOfBoundsException` em cenários de serialização
simultânea.

## Decisão

Expor `JAXBContext` como beans singleton do Spring e criar um `Marshaller` por operação de
serialização.

Um `JAXBContext` é registrado por tipo raiz de XML. Cada serviço XML recebe o `JAXBContext`
correspondente e cria seu próprio `Marshaller` dentro do método de serialização.

## Consequências

Positivas:
- Thread-safe: cada operação usa seu próprio `Marshaller`
- Evita falhas causadas por compartilhamento de estado mutável entre threads
- Compatível com virtual threads do Java 21 ou superior (não depende de estado por thread)
- `JAXBContext` é reutilizado sem compartilhar objetos não thread-safe

Negativas:
- Cria novo `Marshaller` por operação, aumentando a quantidade de objetos de vida curta
- Novos serviços XML devem seguir o mesmo padrão para garantir thread safety

## Notas

Referências:
- [JAXB Thread Safety](https://docs.oracle.com/javase/8/docs/api/javax/xml/bind/JAXBContext.html)
- [Jakarta XML Binding Specification](https://jakarta.ee/specifications/xml-binding/)
