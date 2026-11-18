import { Logger } from '@config/logger.config';
import { AgentContext, AgentResult, AgentType, SwarmConfig, AgentTrace, DEFAULT_SWARM_CONFIG, INTENT_CONFIG } from './agent-types';
import { BaseAgent } from './base.agent';
import { TextAgent } from './text.agent';
import { VisionAgent } from './vision.agent';
import { AudioAgent } from './audio.agent';
import { DocumentAgent } from './document.agent';
import { WebSearchAgent } from './websearch.agent';
import { SchedulerAgent } from './scheduler.agent';
import { RagAgent } from './rag.agent';
import { IntentAgent, IntentLabel } from './intent.agent';
import { SummaryAgent } from './summary.agent';
import { AgentMemory } from './agent-memory';
import { ConfigService } from '@config/env.config';

export interface CoordinatorResult {
  finalResponse: string;
  claudeTextSessionId?: string;
  claudeVisionSessionId?: string;
  conversationHistory: any[];
  agentUsed: AgentType;
  webContext?: string;
  ragContext?: string;
  intent?: IntentLabel;
  metadata: {
    agentsInvolved: AgentType[];
    processingTime: number;
    topology: string;
    strategy: string;
    traces?: AgentTrace[];
    [key: string]: any;
  };
}

export class CoordinatorAgent {
  private readonly logger = new Logger('CoordinatorAgent');
  private readonly agents: Map<AgentType, BaseAgent> = new Map();
  private readonly memory: AgentMemory;
  private readonly summaryAgent: SummaryAgent;
  private readonly intentAgent: IntentAgent;
  private readonly config: SwarmConfig;
  private readonly maxHistoryEntries = 20;
  private readonly summaryThreshold = 20;
  private readonly traces: AgentTrace[] = [];
  private readonly maxStoredTraces = 500;
  private prismaRepository: any;

  constructor(configService?: ConfigService, prismaRepository?: any) {
    this.config = DEFAULT_SWARM_CONFIG;
    this.prismaRepository = prismaRepository;
    this.memory = new AgentMemory(prismaRepository);
    this.summaryAgent = new SummaryAgent();
    this.intentAgent = new IntentAgent();

    this.registerAgent(new TextAgent());
    this.registerAgent(new VisionAgent());
    this.registerAgent(new AudioAgent(configService));
    this.registerAgent(new DocumentAgent());
    this.registerAgent(new WebSearchAgent());
    this.registerAgent(new SchedulerAgent(prismaRepository));
    this.registerAgent(new RagAgent(prismaRepository));
    this.registerAgent(this.intentAgent);
    this.registerAgent(this.summaryAgent);

    for (const agent of this.agents.values()) {
      agent.setCoordinator(this);
    }

    this.logger.log(
      `[Coordinator] Initialized | topology=${this.config.topology} | agents=${this.agents.size} | strategy=${this.config.strategy} | intent=${INTENT_CONFIG.enabled}`,
    );
  }

  private registerAgent(agent: BaseAgent): void {
    this.agents.set(agent.type, agent);
    this.logger.log(`[Coordinator] Registered agent: ${agent.name} (${agent.type})`);
  }

  getAgent(type: AgentType): BaseAgent | undefined {
    return this.agents.get(type);
  }

  listAgents(): BaseAgent[] {
    return Array.from(this.agents.values());
  }

  async route(context: AgentContext): Promise<CoordinatorResult> {
    const startTime = Date.now();
    this.logger.log(
      `[Coordinator] Routing | remoteJid=${context.remoteJid} | contentLen=${context.content?.length || 0}`,
    );

    const agentsInvolved: AgentType[] = [];
    let webContext = '';
    let ragContext = '';
    let intent: IntentLabel | undefined;
    let processedContent = context.content;
    let claudeTextSessionId = context.claudeTextSessionId || '';
    let claudeVisionSessionId = context.claudeVisionSessionId || '';
    let conversationHistory = [...context.conversationHistory];
    let agentUsed: AgentType = 'text';

    const botId = context.bot?.id || '';
    if (botId) {
      try {
        const existingSummary = await this.memory.getConversationSummary(context.remoteJid, botId);
        if (existingSummary) {
          const hasSummaryInHistory = conversationHistory.some(
            (h) => h.role === 'user' && typeof h.content === 'string' && h.content.startsWith('[Resumo de conversa anterior]'),
          );
          if (!hasSummaryInHistory) {
            conversationHistory.unshift({
              role: 'user',
              content: `[Resumo de conversa anterior] ${existingSummary}`,
            });
            this.logger.log(`[Coordinator] Injected memory summary into history (${existingSummary.length} chars)`);
          }
        }
      } catch (err) {
        this.logger.warn(`[Coordinator] Failed to load memory summary: ${err.message}`);
      }
    }

    // Phase 0: Intent classification (fast, guides routing)
    if (INTENT_CONFIG.enabled && this.intentAgent.canHandle(context)) {
      try {
        const intentResult = await this.intentAgent.execute(context);
        if (intentResult.status === 'completed' && intentResult.response) {
          intent = intentResult.response as IntentLabel;
          agentsInvolved.push('intent');
          this.logger.log(`[Coordinator] Phase 0 → IntentClassifier: ${intent}`);
        }
      } catch (err: any) {
        this.logger.warn(`[Coordinator] Intent classification failed: ${err.message}`);
      }
    }

    // Phase 1: Pre-processing agents (audio, document) — transform media into text
    const audioAgent = this.agents.get('audio');
    if (audioAgent && audioAgent.canHandle(context)) {
      agentsInvolved.push('audio');
      this.logger.log(`[Coordinator] Phase 1 → AudioAgent`);
      const audioResult = await audioAgent.execute(context);
      if (audioResult.status === 'completed' && audioResult.response) {
        processedContent = audioResult.response;
        this.logger.log(`[Coordinator] Audio transcribed: ${processedContent.substring(0, 80)}...`);
      } else {
        this.logger.warn(`[Coordinator] Audio agent failed: ${audioResult.error}`);
        return this.buildFailureResult(audioResult.error || 'Audio transcription failed', startTime, agentsInvolved);
      }
    }

    const documentAgent = this.agents.get('document');
    if (documentAgent && documentAgent.canHandle(context)) {
      agentsInvolved.push('document');
      this.logger.log(`[Coordinator] Phase 1 → DocumentAgent`);
      const docContext = { ...context, content: processedContent };
      const docResult = await documentAgent.execute(docContext);
      if (docResult.status === 'completed' && docResult.response) {
        processedContent = docResult.response;
        this.logger.log(`[Coordinator] Document extracted: ${processedContent.substring(0, 80)}...`);
      } else {
        this.logger.warn(`[Coordinator] Document agent failed: ${docResult.error}`);
      }
    }

    // Phase 1.5: Scheduler — intercept reminder commands before LLM processing
    // Skip for document extractions to avoid false matches on document text
    const schedulerAgent = this.agents.get('scheduler');
    const schedulerContext = { ...context, content: processedContent };
    const isDocumentContent = processedContent.startsWith('[document:');
    if (schedulerAgent && !isDocumentContent && schedulerAgent.canHandle(schedulerContext)) {
      agentsInvolved.push('scheduler');
      this.logger.log(`[Coordinator] Phase 1.5 → SchedulerAgent`);
      const schedulerResult = await schedulerAgent.execute(schedulerContext);
      if (schedulerResult.status === 'completed' && schedulerResult.response) {
        this.logger.log(`[Coordinator] Scheduler handled: ${schedulerResult.response.substring(0, 80)}...`);
        return this.buildSuccessResult(
          schedulerResult.response,
          claudeTextSessionId,
          claudeVisionSessionId,
          conversationHistory,
          'scheduler',
          '',
          startTime,
          agentsInvolved,
          schedulerResult.metadata,
        );
      }
    }

    // Phase 2: Parallel — WebSearch + RAG (independent of main processing)
    const webSearchAgent = this.agents.get('websearch');
    const searchContext = { ...context, content: processedContent };
    const shouldSearch = !isDocumentContent && (intent ? (intent === 'search' || intent === 'question') : webSearchAgent?.canHandle(searchContext));
    if (webSearchAgent && shouldSearch) {
      agentsInvolved.push('websearch');
      this.logger.log(`[Coordinator] Phase 2 → WebSearchAgent`);
      const searchResult = await webSearchAgent.execute(searchContext);
      if (searchResult.status === 'completed' && searchResult.response) {
        webContext = searchResult.response;
        this.logger.log(`[Coordinator] Web search OK: ${webContext.substring(0, 120)}...`);
      }
    }

    // Phase 2.5: RAG — retrieve relevant knowledge base chunks
    const ragAgent = this.agents.get('rag');
    const ragContextCtx = { ...context, content: processedContent };
    const shouldRag = intent ? intent === 'question' : true;
    if (ragAgent && shouldRag && ragAgent.canHandle(ragContextCtx)) {
      agentsInvolved.push('rag');
      this.logger.log(`[Coordinator] Phase 2.5 → RagAgent`);
      const ragResult = await ragAgent.execute(ragContextCtx);
      if (ragResult.status === 'completed' && ragResult.response) {
        ragContext = ragResult.response;
        this.logger.log(`[Coordinator] RAG context: ${ragContext.length} chars`);
      }
    }

    // Phase 3: Main processing — Vision or Text (mutually exclusive)
    const combinedContext = [webContext, ragContext].filter(Boolean).join('\n\n');
    const updatedContext: AgentContext = {
      ...context,
      content: processedContent,
      webContext: combinedContext || undefined,
      conversationHistory,
      claudeTextSessionId,
      claudeVisionSessionId,
    };

    const visionAgent = this.agents.get('vision');
    if (visionAgent && visionAgent.canHandle(updatedContext)) {
      agentsInvolved.push('vision');
      agentUsed = 'vision';
      this.logger.log(`[Coordinator] Phase 3 → VisionAgent`);
      const visionResult = await visionAgent.execute(updatedContext);

      if (visionResult.status === 'completed') {
        if (visionResult.sessionId) claudeVisionSessionId = visionResult.sessionId;

        await this.updateConversationHistory(
          conversationHistory,
          context.content,
          visionResult.response,
          context.remoteJid,
          botId,
        );

        return this.buildSuccessResult(
          visionResult.response,
          claudeTextSessionId,
          claudeVisionSessionId,
          conversationHistory,
          agentUsed,
          webContext,
          startTime,
          agentsInvolved,
          visionResult.metadata,
          ragContext,
          intent,
        );
      }

      this.logger.warn(`[Coordinator] Vision failed, falling back to text: ${visionResult.error}`);
    }

    // Fallback / default: Text agent
    const textAgent = this.agents.get('text');
    if (textAgent) {
      agentsInvolved.push('text');
      agentUsed = 'text';
      this.logger.log(`[Coordinator] Phase 3 → TextAgent`);
      const textResult = await textAgent.execute(updatedContext);

      if (textResult.status === 'completed') {
        if (textResult.sessionId) claudeTextSessionId = textResult.sessionId;

        await this.updateConversationHistory(
          conversationHistory,
          processedContent,
          textResult.response,
          context.remoteJid,
          botId,
        );

        if (botId) {
          this.extractFacts(context.remoteJid, botId, processedContent, textResult.response).catch((err) =>
            this.logger.warn(`[Coordinator] Fact extraction failed: ${err.message}`),
          );
        }

        return this.buildSuccessResult(
          textResult.response,
          claudeTextSessionId,
          claudeVisionSessionId,
          conversationHistory,
          agentUsed,
          webContext,
          startTime,
          agentsInvolved,
          textResult.metadata,
          ragContext,
          intent,
        );
      }

      return this.buildFailureResult(textResult.error || 'Text agent failed', startTime, agentsInvolved);
    }

    return this.buildFailureResult('No suitable agent found', startTime, agentsInvolved);
  }

  private readonly factPatterns: { pattern: RegExp; prefKey: string }[] = [
    { pattern: /(?:meu nome é|me chamo|sou o|sou a)\s+([a-zà-ú\s]{2,30})/i, prefKey: 'name' },
    { pattern: /(?:eu gosto de|gosto de|prefiro)\s+(.{2,50})/i, prefKey: 'likes' },
    { pattern: /(?:não gosto de|nao gosto de|odeio)\s+(.{2,50})/i, prefKey: 'dislikes' },
    { pattern: /(?:trabalho na|trabalho no|empresa é|minha empresa é)\s+(.{2,40})/i, prefKey: 'company' },
    { pattern: /(?:moro em|vivo em)\s+([a-zà-ú\s]{2,30})/i, prefKey: 'location' },
    { pattern: /(?:meu (?:whatsapp|telefone|cel) é|me liga no|contato é)\s*([\d\s\-\(\)]{8,20})/i, prefKey: 'phone' },
  ];

  private async extractFacts(remoteJid: string, botId: string, userContent: string, _assistantResponse: string): Promise<void> {
    for (const { pattern, prefKey } of this.factPatterns) {
      const match = userContent.match(pattern);
      if (match && match[1]) {
        const value = match[1].trim().substring(0, 100);
        const existing = await this.memory.retrieve(remoteJid, botId, `pref:${prefKey}`);
        if (existing.length === 0 || existing[0].value !== value) {
          await this.memory.storeUserPreference(remoteJid, botId, prefKey, value);
          this.logger.log(`[Coordinator] Extracted fact: ${prefKey}=${value} for ${remoteJid}`);
        }
      }
    }
  }

  private async updateConversationHistory(
    history: any[],
    userContent: string,
    assistantResponse: string,
    remoteJid: string,
    botId: string,
  ): Promise<void> {
    let historyContent = userContent;
    if (historyContent.includes('|')) {
      const parts = historyContent.split('|');
      historyContent = parts[parts.length - 1];
    }

    history.push({ role: 'user', content: historyContent });
    history.push({ role: 'assistant', content: assistantResponse });

    if (history.length > this.maxHistoryEntries) {
      const { toSummarize, toKeep } = this.summaryAgent.splitHistory(history, this.maxHistoryEntries);

      if (botId && toSummarize.length > 0) {
        try {
          await this.summaryAgent.summarizeAndStore(toSummarize, this.memory, remoteJid, botId);
          this.logger.log(`[Coordinator] Summarized ${toSummarize.length} old messages via SummaryAgent`);
        } catch (err) {
          this.logger.warn(`[Coordinator] Summarization failed, keeping raw history: ${err.message}`);
        }
      }

      history.length = 0;
      history.push(...toKeep);
      this.logger.log(`[Coordinator] History trimmed to ${history.length} entries`);
    }
  }

  private buildSuccessResult(
    response: string,
    textSessionId: string,
    visionSessionId: string,
    history: any[],
    agentUsed: AgentType,
    webContext: string,
    startTime: number,
    agentsInvolved: AgentType[],
    agentMetadata?: any,
    ragContext?: string,
    intent?: IntentLabel,
  ): CoordinatorResult {
    const processingTime = Date.now() - startTime;
    this.logger.log(
      `[Coordinator] ✓ Complete | agent=${agentUsed} | time=${processingTime}ms | agents=[${agentsInvolved.join(',')}] | responseLen=${response.length}`,
    );

    const traces = this.collectTraces(agentsInvolved);
    this.storeTraces(traces);

    return {
      finalResponse: response,
      claudeTextSessionId: textSessionId,
      claudeVisionSessionId: visionSessionId,
      conversationHistory: history,
      agentUsed,
      webContext,
      ragContext,
      intent,
      metadata: {
        agentsInvolved,
        processingTime,
        topology: this.config.topology,
        strategy: this.config.strategy,
        traces,
        ...agentMetadata,
      },
    };
  }

  private buildFailureResult(
    error: string,
    startTime: number,
    agentsInvolved: AgentType[],
  ): CoordinatorResult {
    const processingTime = Date.now() - startTime;
    this.logger.error(`[Coordinator] ✗ Failed | time=${processingTime}ms | error=${error}`);

    const traces = this.collectTraces(agentsInvolved);
    this.storeTraces(traces);

    return {
      finalResponse: `Desculpe, ocorreu um erro: ${error}`,
      conversationHistory: [],
      agentUsed: 'text',
      metadata: {
        agentsInvolved,
        processingTime,
        topology: this.config.topology,
        strategy: this.config.strategy,
        traces,
        error,
      },
    };
  }

  private collectTraces(agentsInvolved: AgentType[]): AgentTrace[] {
    const traces: AgentTrace[] = [];
    for (const agentType of agentsInvolved) {
      const agent = this.agents.get(agentType);
      if (agent && agent.getTraces) {
        traces.push(...agent.getTraces());
      }
    }
    return traces;
  }

  private storeTraces(traces: AgentTrace[], remoteJid?: string, botId?: string): void {
    for (const trace of traces) {
      this.traces.push(trace);
    }
    if (this.traces.length > this.maxStoredTraces) {
      this.traces.splice(0, this.traces.length - this.maxStoredTraces);
    }

    if (this.prismaRepository?.agentTrace) {
      for (const trace of traces) {
        this.prismaRepository.agentTrace.create({
          data: {
            remoteJid: remoteJid || null,
            botId: botId || null,
            agentType: trace.agentType,
            inputLength: trace.inputLength,
            outputLength: trace.outputLength,
            latencyMs: trace.latencyMs,
            model: trace.model || null,
            success: trace.success,
            error: trace.error || null,
            topology: this.config.topology,
            strategy: this.config.strategy,
          },
        }).catch((err: any) => {
          this.logger.warn(`[Coordinator] Failed to store trace: ${err.message}`);
        });
      }
    }
  }

  getTraces(): AgentTrace[] {
    return [...this.traces];
  }

  getAgentMetrics(): Record<string, { count: number; avgLatencyMs: number; failures: number }> {
    const metrics: Record<string, { count: number; totalLatency: number; failures: number }> = {};
    for (const trace of this.traces) {
      const key = trace.agentType;
      if (!metrics[key]) metrics[key] = { count: 0, totalLatency: 0, failures: 0 };
      metrics[key].count++;
      metrics[key].totalLatency += trace.latencyMs;
      if (!trace.success) metrics[key].failures++;
    }
    const result: Record<string, { count: number; avgLatencyMs: number; failures: number }> = {};
    for (const [key, val] of Object.entries(metrics)) {
      result[key] = {
        count: val.count,
        avgLatencyMs: val.count > 0 ? Math.round(val.totalLatency / val.count) : 0,
        failures: val.failures,
      };
    }
    return result;
  }

  getMemory(): AgentMemory {
    return this.memory;
  }

  getRagAgent(): RagAgent | undefined {
    return this.agents.get('rag') as RagAgent | undefined;
  }

  getSummaryAgent(): SummaryAgent {
    return this.summaryAgent;
  }

  getIntentAgent(): IntentAgent {
    return this.intentAgent;
  }
}
