import { Integration } from '@api/types/wa.types';
import { AgentContext, AgentResult, OLLAMA_CONFIG, VISION_MODEL_CHAIN } from './agent-types';
import { BaseAgent } from './base.agent';
import { claudeCLI } from './claude-cli';
import axios from 'axios';
import { downloadMediaMessage } from 'baileys';
import P from 'pino';

export class VisionAgent extends BaseAgent {
  constructor() {
    super({
      type: 'vision',
      name: 'Vision Agent',
      capabilities: ['image-analysis', 'ocr', 'image-description'],
      model: OLLAMA_CONFIG.visionModel,
    });
  }

  canHandle(context: AgentContext): boolean {
    return context.content.includes('imageMessage');
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const imageDataUri = await this.downloadImageAsBase64(context.msg, context.instance);
      if (!imageDataUri) {
        return this.failure('Could not download image');
      }

      const dataUriMatch = imageDataUri.match(/^data:([^;]+);base64,(.+)$/);
      if (!dataUriMatch) {
        return this.failure('Could not parse image dataUri');
      }

      const imageMimeType = dataUriMatch[1];
      const imageBase64 = dataUriMatch[2];

      const media = context.content.split('|');
      const imageCaption = media[2] || 'Descreva e analise esta imagem.';

      const bot = context.bot;
      const systemMessages: string[] = bot.systemMessages || [];
      const assistantMessages: string[] = bot.assistantMessages || [];
      const userMessages: string[] = bot.userMessages || [];

      const systemParts = [...systemMessages];
      systemParts.push(`O usuário enviou uma imagem com a legenda: "${imageCaption}". Analise a imagem e responda em português.`);
      const systemPrompt = this.buildSystemPrompt(systemParts.join('\n'), context.webContext, context.pushName);

      const hasResume = !!context.claudeVisionSessionId;
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

      let lastError: any = null;
      let result = null;

      for (const visionModel of VISION_MODEL_CHAIN) {
        try {
          this.logger.log(`[VisionAgent] Trying model: ${visionModel} | session=${context.claudeVisionSessionId || 'new'}`);
          result = await claudeCLI.executeImage(
            userPrompt,
            systemPrompt,
            visionModel,
            imageBase64,
            imageMimeType,
            context.claudeVisionSessionId,
          );
          this.logger.log(`[VisionAgent] Success with ${visionModel}`);
          break;
        } catch (err: any) {
          lastError = err;
          this.logger.warn(`[VisionAgent] Model ${visionModel} failed: ${err.message}`);
          continue;
        }
      }

      if (context.instance.integration === Integration.WHATSAPP_BAILEYS) {
        await context.instance.client.sendPresenceUpdate('paused', context.remoteJid);
      }

      if (!result) {
        return this.failure(`All vision models failed. Last: ${lastError?.message}`, {
          processingTime: Date.now() - startTime,
        });
      }

      return this.success(result.response, result.sessionId, {
        model: VISION_MODEL_CHAIN.join(','),
        imageCaption,
        processingTime: Date.now() - startTime,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Vision agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  private async downloadImageAsBase64(msg: any, instance: any): Promise<string | null> {
    try {
      const imageMsg = msg.message?.imageMessage;
      if (!imageMsg) {
        this.logger.warn('[VisionAgent] Image message object not found');
        return null;
      }

      const mimetype: string = imageMsg.mimetype || 'image/jpeg';
      const fileSize: number = imageMsg.fileLength || 0;

      if (fileSize > 5 * 1024 * 1024) {
        this.logger.warn(`[VisionAgent] Image too large: ${fileSize} bytes`);
        return null;
      }

      let fileBuffer: Buffer;

      if (msg.message.mediaUrl) {
        const response = await axios.get(msg.message.mediaUrl, {
          responseType: 'arraybuffer',
          timeout: 30000,
        });
        fileBuffer = Buffer.from(response.data, 'binary');
      } else if (msg.message.base64) {
        fileBuffer = Buffer.from(msg.message.base64, 'base64');
      } else {
        fileBuffer = await downloadMediaMessage(
          { key: msg.key, message: msg.message },
          'buffer',
          {},
          {
            logger: P({ level: 'error' }) as any,
            reuploadRequest: instance,
          },
        );
      }

      const base64 = fileBuffer.toString('base64');
      return `data:${mimetype};base64,${base64}`;
    } catch (error: any) {
      this.logger.error(`[VisionAgent] Error downloading image: ${error.message}`);
      return null;
    }
  }
}
