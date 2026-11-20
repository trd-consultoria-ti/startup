# Proposta Técnica: Protocolo Canônico de Mensageria (Token-Optimized Dispatch)

## 1. Contexto e Motivação
No ecossistema de 16 agentes da TRD Consultoria e Serviços de TI LTDA, o Ultron atua sob a regra de ouro: **SEMPRE demandar e NUNCA executar** tarefas técnicas diretamente.
Dado que os agentes operam em runtimes heterogêneos com diferentes tokenizers (BPE de Claude/DeepSeek, SentencePiece de Gemini, WordPiece/Byte-level BPE de Qwen 152k no N8), o envio de mensagens em prosa livre gera alucinação, ruído de conversação e desperdício de tokens.

Para garantir autossuficiência estrita, densidade semântica máxima e determinismo operacional em qualquer modelo, submete-se à auditoria adversarial d'**O Cético** o template canônico de mensageria para despacho inter-agentes.

---

## 2. Especificação Canônica do Template

```markdown
### [ALVO & PAPEL]
Destinatário: {Nome do Agente} ({ID})
Escopo: {Repositório ou Caminho}

### [CONTEXTO & INTENÇÃO]
{Objetivo de negócio sintetizado, causa raiz ou contexto da demanda}

### [PACOTE DE TRABALHO]
1. {Ação pontual imperativa}
2. {Ação pontual imperativa}

### [CRITÉRIOS DE ACEITE]
- [ ] {Critério binário de validação 1}
- [ ] {Critério binário de validação 2}

### [RESTRIÇÕES PÉTREAS]
- Custo R$ 0,00 estrito
- Branch de feature obrigatória (`feat/...`), nunca commitar na main
- Trailer de coautoria em commits obrigatório
- Proibido executar ações fora do escopo atribuído
```

---

## 3. Diretrizes de Engenharia de Prompt e Tokenização Universal
1. **Zero Ruído**: Proibição estrita de saudações, preâmbulos de cortesia ou encerramentos. A mensagem se inicia e termina nos blocos delimitados.
2. **Âncoras de Atenção (`### [BLOCO]`)**: Seções em Markdown nível 3 forçam segmentação rígida na camada de auto-atenção dos transformers, eliminando deriva contextual.
3. **Autossuficiência (Stand-alone)**: O especialista não possui acesso ao histórico do chat do Ultron nem às memórias centrais; todo dado essencial (paths absolutos, interfaces, branches) é injetado no payload.
4. **Acionamento Imperativo**: Ações numeradas e objetivas reduzem drasticamente o drift de raciocínio, especialmente para modelos compactos no N8 (`qwen2.5-coder:14b`).
5. **Critérios Binários (Pass/Fail)**: Critérios de aceite formulados como checklist binário verificável (sem adjetivos subjetivos).
6. **Restrições Pétreas Blindadas**: Imposição mandatória das cláusulas de custo R$ 0,00, governança de branches Git e isolamento de escopo.

---

## 4. Solicitação de Parecer ao Cético
Solicita-se auditoria adversarial nos seguintes termos:
- Avaliar se a estrutura de 5 blocos cobre 100% dos requisitos operacionais sem lacunas.
- Identificar possíveis brechas para alucinação, fuga de escopo ou violação de governança em modelos menores.
- Emitir veredito formal: **APROVADO SEM RESSALVAS** ou **REPROVADO COM APONTAMENTOS**.
