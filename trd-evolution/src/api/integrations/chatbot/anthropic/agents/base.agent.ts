import { Logger } from '@config/logger.config';
import { AgentType, AgentStatus, AgentContext, AgentResult, AgentConfig, AgentTrace, CIRCUIT_BREAKER_CONFIG } from './agent-types';

export abstract class BaseAgent {
  protected readonly logger: Logger;
  readonly type: AgentType;
  readonly name: string;
  readonly capabilities: string[];
  protected status: AgentStatus = 'idle';
  protected config: AgentConfig;

  private failureCount = 0;
  private lastFailureTime = 0;
  private circuitOpen = false;
  private readonly traces: AgentTrace[] = [];
  private readonly maxTraces = 100;
  protected coordinator?: any;

  constructor(config: AgentConfig) {
    this.config = config;
    this.type = config.type;
    this.name = config.name;
    this.capabilities = config.capabilities || [];
    this.logger = new Logger(`Agent:${config.name}`);
  }

  setCoordinator(coordinator: any): void {
    this.coordinator = coordinator;
  }

  getStatus(): AgentStatus {
    return this.status;
  }

  isCircuitOpen(): boolean {
    if (this.circuitOpen) {
      if (Date.now() - this.lastFailureTime > CIRCUIT_BREAKER_CONFIG.resetTimeoutMs) {
        this.circuitOpen = false;
        this.failureCount = 0;
        this.logger.log(`[${this.name}] Circuit breaker reset`);
        return false;
      }
      return true;
    }
    return false;
  }

  protected recordFailure(): void {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    if (this.failureCount >= CIRCUIT_BREAKER_CONFIG.failureThreshold) {
      this.circuitOpen = true;
      this.logger.warn(`[${this.name}] Circuit breaker opened after ${this.failureCount} failures`);
    }
  }

  protected recordSuccess(): void {
    this.failureCount = 0;
    this.circuitOpen = false;
  }

  protected async retryWithBackoff<T>(
    fn: () => Promise<T>,
    attempts: number = CIRCUIT_BREAKER_CONFIG.retryAttempts,
    backoffMs: number = CIRCUIT_BREAKER_CONFIG.retryBackoffMs,
  ): Promise<T> {
    let lastError: any;
    for (let i = 0; i < attempts; i++) {
      try {
        return await fn();
      } catch (err: any) {
        lastError = err;
        if (i < attempts - 1) {
          const delay = backoffMs * Math.pow(2, i);
          this.logger.warn(`[${this.name}] Retry ${i + 1}/${attempts} after ${delay}ms: ${err.message}`);
          await new Promise((r) => setTimeout(r, delay));
        }
      }
    }
    throw lastError;
  }

  protected async delegate(to: AgentType, context: AgentContext): Promise<AgentResult | null> {
    if (!this.coordinator) {
      this.logger.warn(`[${this.name}] Cannot delegate — no coordinator set`);
      return null;
    }
    const agent = this.coordinator.getAgent(to);
    if (!agent) {
      this.logger.warn(`[${this.name}] Cannot delegate — agent ${to} not found`);
      return null;
    }
    if (agent.isCircuitOpen?.()) {
      this.logger.warn(`[${this.name}] Cannot delegate — agent ${to} circuit open`);
      return null;
    }
    if (!agent.canHandle(context)) {
      return null;
    }
    this.logger.log(`[${this.name}] Delegating to ${to}`);
    return await agent.execute(context);
  }

  getTraces(): AgentTrace[] {
    return [...this.traces];
  }

  protected recordTrace(trace: AgentTrace): void {
    this.traces.push(trace);
    if (this.traces.length > this.maxTraces) {
      this.traces.shift();
    }
  }

  canHandle(context: AgentContext): boolean {
    return true;
  }

  abstract execute(context: AgentContext): Promise<AgentResult>;

  protected success(response: string, sessionId?: string, metadata?: any): AgentResult {
    this.status = 'completed';
    this.recordSuccess();
    this.recordTrace({
      agentType: this.type,
      inputLength: metadata?.inputLength || 0,
      outputLength: response.length,
      latencyMs: metadata?.processingTime || 0,
      model: metadata?.model,
      success: true,
      timestamp: Date.now(),
    });
    return {
      response,
      sessionId,
      status: 'completed',
      metadata: {
        agentType: this.type,
        ...metadata,
      },
    };
  }

  protected failure(error: string, metadata?: any): AgentResult {
    this.status = 'failed';
    this.recordFailure();
    this.logger.error(`Agent ${this.name} failed: ${error}`);
    this.recordTrace({
      agentType: this.type,
      inputLength: metadata?.inputLength || 0,
      outputLength: 0,
      latencyMs: metadata?.processingTime || 0,
      model: metadata?.model,
      success: false,
      error,
      timestamp: Date.now(),
    });
    return {
      response: '',
      status: 'failed',
      error,
      metadata: {
        agentType: this.type,
        ...metadata,
      },
    };
  }

  protected buildSystemPrompt(basePrompt: string, webContext?: string, pushName?: string): string {
    const parts: string[] = [basePrompt];

    if (pushName) {
      parts.push(`O nome do usuário que está conversando com você é: ${pushName}. Use o nome dele ao se dirigir ao usuário.`);
    }

    if (webContext) {
      parts.push(
        `A busca web JÁ FOI FEITA. NÃO tente usar ferramentas de busca. NÃO tente pesquisar. Os resultados já estão abaixo. Sua única tarefa é responder ao usuário usando os resultados.\n\nINSTRUÇÕES:\n- Use os resultados da busca abaixo para responder diretamente\n- Para músicas/vídeos: liste o título + link de cada vídeo encontrado\n- Para informações factuais (tabelas, preços, resultados): transcreva os dados diretamente da fonte\n- Cite a fonte quando relevante\n- NÃO diga "vou buscar" ou "deixe-me pesquisar" — a busca já foi feita\n\nREGRAS OBRIGATÓRIAS:\n- NUNCA mencione skills, ferramentas, plugins ou processos internos (ex: "Usando superpowers: brainstorming", "Deixe-me usar X ferramenta"). O usuário não precisa saber o que acontece internamente.\n- NUNCA diga "vou pensar", "deixe-me analisar", "usando X para entender". Simplesmente responda diretamente.\n- Responda como uma pessoa conversando no WhatsApp, sem expor metacognição ou fluxo de raciocínio.\n\nFORMATO: Responda em texto simples para WhatsApp. NÃO use tabelas markdown. Prefira texto corrido (parágrafos) em vez de bullet points. Bullet points só para listas curtas quando fizer sentido. Negrito com *texto* só em palavras-chave. Seja conciso e conversacional.\n\n--- RESULTADOS DA BUSCA ---\n${webContext.substring(0, 12000)}\n--- FIM DOS RESULTADOS ---`,
      );
    } else {
      parts.push(
        'Você é um assistente de WhatsApp conversando com uma pessoa real.\n\nREGRAS OBRIGATÓRIAS:\n- NUNCA mencione skills, ferramentas, plugins ou processos internos. O usuário não precisa saber o que acontece internamente.\n- NUNCA diga "vou pensar", "deixe-me analisar", "usando X para entender". Simplesmente responda diretamente.\n- Responda como uma pessoa conversando no WhatsApp, sem expor metacognição ou fluxo de raciocínio.\n\nESTILO DE CONVERSA:\n- Seja CONVERSACIONAL, não enciclopédico. Fale como uma pessoa inteligente conversando num chat, não como um artigo da Wikipedia.\n- ADAPTE o tom ao do usuário. Se ele escreve casual/informal ("ta mais vc viu", abreviações, sem acentos), responda no mesmo registro. Se ele for formal, seja formal.\n- NÃO use bullet points em toda resposta. Use parágrafos normais de texto. Bullet points só quando realmente fizer sentido (listas curtas, opções).\n- NÃO use negrito excessivo. Negrito só em palavras-chave importantes, não em cada termo.\n- Seja conciso. WhatsApp é chat, não é artigo. Respostas longas demais cansam o leitor.\n- Quando o usuário fizer uma pergunta casual ou brincadeira (ex: "vc viu exterminador do futuro?"), responda de forma leve e conversacional, não com uma lista enciclopédica de fatos.\n- Use texto corrido para explicar coisas. Liste fatos só quando for uma lista literal.\n- Em português brasileiro.',
      );
    }

    return parts.join('\n\n').substring(0, 20000);
  }

  protected buildUserPrompt(
    assistantMessages: string[],
    userMessages: string[],
    conversationHistory: any[],
    content: string,
    hasResume: boolean,
    pushName?: string,
  ): string {
    const parts: string[] = [];

    for (const m of assistantMessages) parts.push(`Assistant: ${m}`);
    for (const m of userMessages) parts.push(`User: ${m}`);

    if (!hasResume && conversationHistory.length > 0) {
      parts.push('--- Histórico da conversa ---');
      for (const h of conversationHistory) {
        let hContent = typeof h.content === 'string' ? h.content : JSON.stringify(h.content);
        if (hContent.includes('|')) {
          const segs = hContent.split('|');
          hContent = segs[segs.length - 1];
        }
        const truncated = hContent.substring(0, 3000);
        if (h.role === 'user') parts.push(`User: ${truncated}`);
        if (h.role === 'assistant') parts.push(`Assistant: ${truncated}`);
      }
      parts.push('--- Fim do histórico ---');
    }

    let cleanContent = content;
    if (cleanContent.includes('|')) {
      const segs = cleanContent.split('|');
      cleanContent = segs[segs.length - 1];
    }
    parts.push(cleanContent);

    parts.push('\n\n[LEMBRETE DE ESTILO: Responda como uma pessoa real no WhatsApp. Texto corrido, NÃO bullet points. Adapte o tom ao do usuário. Seja conciso. Não exponha processos internos.]');

    return parts.join('\n\n');
  }

  protected cleanContent(content: string): string {
    if (content.includes('|')) {
      const parts = content.split('|');
      return parts[parts.length - 1];
    }
    return content;
  }
}
