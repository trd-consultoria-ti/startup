import { AgentContext, AgentResult, INTENT_CONFIG, TEXT_MODEL_CHAIN } from './agent-types';
import { BaseAgent } from './base.agent';
import { claudeCLI } from './claude-cli';

export type IntentLabel = 'reminder' | 'search' | 'question' | 'conversation' | 'image' | 'audio' | 'document' | 'trd_console_lead' | 'unknown';

export interface IntentResult {
  label: IntentLabel;
  confidence: number;
  rawResponse: string;
}

const INTENT_MAP: Record<string, IntentLabel> = {
  reminder: 'reminder',
  schedule: 'reminder',
  alarm: 'reminder',
  lembrete: 'reminder',
  agenda: 'reminder',
  search: 'search',
  google: 'search',
  youtube: 'search',
  buscar: 'search',
  pesquisar: 'search',
  question: 'question',
  pergunta: 'question',
  duvida: 'question',
  dúvida: 'question',
  conversation: 'conversation',
  chat: 'conversation',
  conversa: 'conversation',
  image: 'image',
  imagem: 'image',
  foto: 'image',
  audio: 'audio',
  voz: 'audio',
  document: 'document',
  documento: 'document',
  pdf: 'document',
  arquivo: 'document',
  trd: 'trd_console_lead',
  console: 'trd_console_lead',
  especificacao: 'trd_console_lead',
  briefing: 'trd_console_lead',
};

export class IntentAgent extends BaseAgent {
  private cache = new Map<string, { label: IntentLabel; ts: number }>();
  private readonly cacheTtlMs = 30_000;

  constructor() {
    super({
      type: 'intent',
      name: 'Intent Classifier',
      capabilities: ['classification', 'routing', 'fast-inference'],
    });
  }

  canHandle(context: AgentContext): boolean {
    return INTENT_CONFIG.enabled && (context.content || '').length > 0;
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const content = this.cleanContent(context.content);
      const cacheKey = `${context.remoteJid}:${content.substring(0, 100)}`;

      const cached = this.cache.get(cacheKey);
      if (cached && Date.now() - cached.ts < this.cacheTtlMs) {
        return this.success(cached.label, undefined, {
          processingTime: Date.now() - startTime,
          intent: cached.label,
          cached: true,
        });
      }

      const label = await this.classify(content);

      this.cache.set(cacheKey, { label, ts: Date.now() });

      return this.success(label, undefined, {
        processingTime: Date.now() - startTime,
        intent: label,
        model: INTENT_CONFIG.model,
        cached: false,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Intent classification failed', {
        processingTime: Date.now() - startTime,
        intent: 'unknown' as IntentLabel,
      });
    }
  }

  async classify(content: string): Promise<IntentLabel> {
    const lower = content.toLowerCase().trim();

    if (
      content.includes('TRD CONSULTORIA - ESPECIFICAÇÃO DE ENGENHARIA') ||
      content.includes('TRD-ENG-') ||
      (content.includes('ESPECIFICAÇÃO DE ENGENHARIA') && content.includes('TRD')) ||
      (lower.includes('trd consultoria') && (lower.includes('modalidade') || lower.includes('escopo')))
    ) {
      return 'trd_console_lead';
    }

    if (content.includes('imageMessage')) return 'image';
    if (content.includes('audioMessage')) return 'audio';
    if (content.includes('documentMessage') || content.includes('documentWithCaption')) return 'document';

    const fastModel = TEXT_MODEL_CHAIN[TEXT_MODEL_CHAIN.length - 1] || INTENT_CONFIG.model;

    const prompt = `Classifique a intenção da mensagem em UMA palavra: reminder, search, question, conversation.

Mensagem: "${content.substring(0, 500)}"

Regras:
- reminder: o usuário quer agendar ou ser lembrado de algo (lembre, lembra, lembrar, lembrete, avise, avisa, avisar, alarme, agenda, agendar, alerta)
- search: o usuário EXPLICITAMENTE pede pra buscar algo (buscar, pesquisar, google, youtube, procure por, ache vídeos sobre). NÃO é search se o usuário só está conversando sobre tecnologia, IA, notícias ou qualquer assunto.
- question: pergunta factual ou necessidade de informação em tempo real (o que é, quem é, como funciona, resumo, resumir, analise, tabela, classificação, placar, resultado, preço, cotação, clima, notícia, lançamento, eleição, ao vivo, onde assistir, que horas, quando é)
- conversation: chat geral, saudação, opinião, texto longo sem pedido explícito, conversa casual, filosofia, reflexão

IMPORTANTE: Se o usuário enviou um texto longo (opinião, reflexão, parágrafo) sem pedir explicitamente pra buscar/pesquisar, classifique como conversation ou question, NUNCA search.

Exemplos:
- "busque vídeos sobre IA" → search
- "pesquise sobre automação" → search
- "me lembra de tomar água" → reminder
- "lembre de pagar a conta amanhã" → reminder
- "No mundo digital de hoje a IA transforma tudo" → conversation (NÃO é search)
- "o que é machine learning?" → question
- "qual a tabela do brasileirão?" → question
- "qual o preço da gasolina hoje?" → question
- "que horas é o jogo de hoje?" → question
- "bom dia" → conversation
- "vamos filosofar sobre IA" → conversation

Responda APENAS a palavra da categoria.`;

    try {
      const result = await this.retryWithBackoff(
        async () => {
          const r = await claudeCLI.executeText(prompt, 'Você é um classificador de intenção. Responda apenas uma palavra.', fastModel);
          return r;
        },
        2,
        500,
      );

      const raw = result.response.toLowerCase().trim();
      const label = INTENT_MAP[raw] || this.heuristicClassify(lower);
      
      // Post-LLM correction: if LLM says conversation but message has question indicators, override
      if (label === 'conversation' && this.looksLikeQuestion(lower)) {
        this.logger.log(`[IntentAgent] LLM said conversation, overriding to question (question indicators detected)`);
        return 'question';
      }
      
      return label;
    } catch (err: any) {
      this.logger.warn(`[IntentAgent] LLM classify failed: ${err.message} — using heuristic`);
      return this.heuristicClassify(lower);
    }
  }

  private heuristicClassify(lower: string): IntentLabel {
    if (
      lower.includes('trd consultoria') ||
      lower.includes('trd-eng-') ||
      lower.includes('especificação de engenharia') ||
      lower.includes('especificacao de engenharia')
    ) {
      return 'trd_console_lead';
    }
    if (['lembre', 'lembra', 'lembrar', 'lembrete', 'lembrança', 'avise', 'avisa', 'avisar', 'aviso', 'agenda', 'agendar', 'alarme', 'alerta', 'alertar'].some((k) => lower.includes(k))) {
      return 'reminder';
    }
    const searchKeywords = ['buscar', 'busque', 'pesquisar', 'pesquise', 'procurar', 'procure', 'busca ', 'google ', 'youtube '];
    const isShortMessage = lower.length < 200;
    const startsWithSearchVerb = /^(buscar|busque|busco|pesquisar|pesquise|pesquisa|procurar|procure|procura|busca|ache|encontre|procurar)\b/.test(lower);
    if (startsWithSearchVerb || (isShortMessage && searchKeywords.some((k) => lower.includes(k)))) {
      return 'search';
    }
    if (
      this.looksLikeQuestion(lower) ||
      ['o que é', 'quem é', 'como funciona', 'qual o', 'qual a', 'quais as', 'quais os', 'quais foram', 'quando', 'onde', 'resumo', 'resumir', 'resuma', 'analise', 'analisa', 'quanto', 'custa', 'custo', 'valor', 'preço', 'preco', 'orçamento', 'orcamento', 'pagamento', 'condições', 'condicoes', 'desenvolver', 'desenvolvimento', 'sistema', 'software', 'aplicativo', 'garantia', 'suporte', 'contrato'].some((k) => lower.includes(k))
    ) {
      return 'question';
    }
    return 'conversation';
  }

  private looksLikeQuestion(lower: string): boolean {
    const questionIndicators = [
      'quais', 'qual', 'quem', 'quando', 'onde', 'como', 'o que', 'que ',
      'notícia', 'noticia', 'notícias', 'noticias',
      'hoje', 'agora', 'atual', 'recente', 'último', 'ultimo', 'ontem',
      'principais', 'melhores', 'piores', 'maiores',
      'preço', 'preco', 'cotação', 'cotacao', 'valor',
      'resultado', 'placar', 'tabela', 'classificação', 'classificacao',
      'clima', 'temperatura', 'previsão', 'previsao',
      'lançamento', 'lancamento', 'lançou', 'lancou', 'saiu', 'estreiou',
      'quantos', 'quantas', 'existe', 'existem', 'tem como', 'é possível', 'e possivel',
    ];
    return questionIndicators.some((k) => lower.includes(k));
  }

  clearCache(): void {
    this.cache.clear();
  }
}
