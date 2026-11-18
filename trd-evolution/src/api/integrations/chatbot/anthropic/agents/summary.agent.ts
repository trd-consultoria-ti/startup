import { AgentContext, AgentResult, TEXT_MODEL_CHAIN } from './agent-types';
import { BaseAgent } from './base.agent';
import { claudeCLI } from './claude-cli';
import { AgentMemory } from './agent-memory';

export class SummaryAgent extends BaseAgent {
  private readonly summaryThreshold = 20;
  private readonly maxSummaryWords = 500;

  constructor() {
    super({
      type: 'summary',
      name: 'Summary Agent',
      capabilities: ['summarization', 'context-compression', 'conversation-memory'],
    });
  }

  canHandle(context: AgentContext): boolean {
    return false;
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const history = context.conversationHistory || [];
      const summary = await this.summarize(history, null, context.remoteJid, context.bot?.id || '');

      return this.success(summary, undefined, {
        processingTime: Date.now() - startTime,
        summarizedEntries: history.length,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Summary agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  async summarize(
    history: any[],
    existingSummary: string | null,
    remoteJid: string,
    botId: string,
  ): Promise<string> {
    if (history.length === 0) return existingSummary || '';

    const summaryPrompt = history
      .map((h) => `${h.role}: ${typeof h.content === 'string' ? h.content.substring(0, 500) : ''}`)
      .join('\n');

    const fullPrompt = existingSummary
      ? `Resumo anterior: ${existingSummary}\n\nNovas mensagens:\n${summaryPrompt}\n\nCrie um resumo conciso e atualizado desta conversa, mantendo informações importantes como preferências, decisões e contexto. Máximo ${this.maxSummaryWords} palavras.`
      : `Resuma a seguinte conversa de forma concisa, mantendo informações importantes como preferências, decisões e contexto. Máximo ${this.maxSummaryWords} palavras.\n\n${summaryPrompt}`;

    const model = TEXT_MODEL_CHAIN[0] || 'qwen2.5:3b';

    const result = await this.retryWithBackoff(
      async () => {
        return await claudeCLI.executeText(
          fullPrompt,
          'Você é um assistente especializado em resumir conversas. Seja conciso e mantenha apenas informações relevantes.',
          model,
        );
      },
      2,
      1000,
    );

    return result.response;
  }

  async summarizeAndStore(
    history: any[],
    memory: AgentMemory,
    remoteJid: string,
    botId: string,
  ): Promise<string | null> {
    if (history.length < this.summaryThreshold) return null;

    try {
      const existingSummary = await memory.getConversationSummary(remoteJid, botId);
      const summary = await this.summarize(history, existingSummary, remoteJid, botId);

      await memory.storeConversationSummary(remoteJid, botId, summary);
      this.logger.log(`[SummaryAgent] Stored summary (${summary.length} chars) for ${remoteJid}`);

      return summary;
    } catch (err: any) {
      this.logger.warn(`[SummaryAgent] Summarization failed: ${err.message}`);
      return null;
    }
  }

  shouldSummarize(historyLength: number, maxEntries: number): boolean {
    return historyLength > maxEntries;
  }

  splitHistory(history: any[], maxEntries: number): { toSummarize: any[]; toKeep: any[] } {
    const toSummarize = history.slice(0, history.length - maxEntries + 4);
    const toKeep = history.slice(history.length - maxEntries + 4);
    return { toSummarize, toKeep };
  }
}
