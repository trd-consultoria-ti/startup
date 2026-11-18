import { Integration } from '@api/types/wa.types';
import { AgentContext, AgentResult, OLLAMA_CONFIG, TEXT_MODEL_CHAIN } from './agent-types';
import { BaseAgent } from './base.agent';
import { claudeCLI } from './claude-cli';

export class TextAgent extends BaseAgent {
  constructor() {
    super({
      type: 'text',
      name: 'Text Agent',
      capabilities: ['text-processing', 'conversation', 'reasoning'],
      model: OLLAMA_CONFIG.textModel,
    });
  }

  canHandle(context: AgentContext): boolean {
    const content = context.content || '';
    return !content.includes('imageMessage') &&
      !content.includes('audioMessage') &&
      !content.includes('documentMessage') &&
      !content.includes('documentWithCaption');
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const bot = context.bot;
      const systemMessages: string[] = bot.systemMessages || [];
      const assistantMessages: string[] = bot.assistantMessages || [];
      const userMessages: string[] = bot.userMessages || [];

      const systemPrompt = this.buildSystemPrompt(systemMessages.join('\n'), context.webContext, context.pushName);
      const hasResume = !!context.claudeTextSessionId;

      const userPrompt = this.buildUserPrompt(
        assistantMessages,
        userMessages,
        context.conversationHistory,
        context.content,
        hasResume,
        context.pushName,
      );

      if (context.instance.integration === Integration.WHATSAPP_BAILEYS) {
        await context.instance.client.presenceSubscribe(context.remoteJid);
        await context.instance.client.sendPresenceUpdate('composing', context.remoteJid);
      }

      this.logger.log(`[TextAgent] Executing | models=${TEXT_MODEL_CHAIN.join(',')} | promptLen=${userPrompt.length} | resume=${hasResume}`);

      let lastError: any = null;
      let result = null;
      let usedModel = '';

      for (const textModel of TEXT_MODEL_CHAIN) {
        try {
          this.logger.log(`[TextAgent] Trying model: ${textModel} | session=${context.claudeTextSessionId || 'new'}`);
          result = await claudeCLI.executeText(
            userPrompt,
            systemPrompt,
            textModel,
            context.claudeTextSessionId,
          );
          usedModel = textModel;
          this.logger.log(`[TextAgent] Success with ${textModel}`);
          break;
        } catch (err: any) {
          lastError = err;
          this.logger.warn(`[TextAgent] Model ${textModel} failed: ${err.message}`);
          continue;
        }
      }

      if (context.instance.integration === Integration.WHATSAPP_BAILEYS) {
        await context.instance.client.sendPresenceUpdate('paused', context.remoteJid);
      }

      if (!result) {
        return this.failure(`All text models failed. Last: ${lastError?.message}`, {
          processingTime: Date.now() - startTime,
        });
      }

      return this.success(result.response, result.sessionId, {
        model: usedModel,
        processingTime: Date.now() - startTime,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Text agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }
}
