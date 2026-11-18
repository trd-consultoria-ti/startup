import { InstanceDto } from '@api/dto/instance.dto';
import { PrismaRepository } from '@api/repository/repository.service';
import { WAMonitoringService } from '@api/services/monitor.service';
import { Integration } from '@api/types/wa.types';
import { ConfigService } from '@config/env.config';
import { Logger } from '@config/logger.config';
import { IntegrationSession } from '@prisma/client';

export abstract class BaseChatbotService<BotType = any, SettingsType = any> {
  protected readonly logger: Logger;
  protected readonly waMonitor: WAMonitoringService;
  protected readonly prismaRepository: PrismaRepository;
  protected readonly configService?: ConfigService;

  constructor(
    waMonitor: WAMonitoringService,
    prismaRepository: PrismaRepository,
    loggerName: string,
    configService?: ConfigService,
  ) {
    this.waMonitor = waMonitor;
    this.prismaRepository = prismaRepository;
    this.logger = new Logger(loggerName);
    this.configService = configService;
  }

  protected isImageMessage(content: string): boolean {
    return content.includes('imageMessage');
  }

  protected isAudioMessage(content: string): boolean {
    return content.includes('audioMessage');
  }

  protected isJSON(str: string): boolean {
    try {
      JSON.parse(str);
      return true;
    } catch {
      return false;
    }
  }

  protected getMediaType(url: string): string | null {
    const extension = url.split('.').pop()?.toLowerCase();
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
    const audioExtensions = ['mp3', 'wav', 'aac', 'ogg'];
    const videoExtensions = ['mp4', 'avi', 'mkv', 'mov'];
    const documentExtensions = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'];

    if (imageExtensions.includes(extension || '')) return 'image';
    if (audioExtensions.includes(extension || '')) return 'audio';
    if (videoExtensions.includes(extension || '')) return 'video';
    if (documentExtensions.includes(extension || '')) return 'document';
    return null;
  }

  public async createNewSession(instance: InstanceDto | any, data: any, type: string) {
    try {
      const pushNameValue =
        typeof data.pushName === 'object' && data.pushName?.pushName
          ? data.pushName.pushName
          : typeof data.pushName === 'string'
            ? data.pushName
            : null;

      const remoteJidValue =
        typeof data.remoteJid === 'object' && data.remoteJid?.remoteJid ? data.remoteJid.remoteJid : data.remoteJid;

      const session = await this.prismaRepository.integrationSession.create({
        data: {
          remoteJid: remoteJidValue,
          pushName: pushNameValue,
          sessionId: remoteJidValue,
          status: 'opened',
          awaitUser: false,
          botId: data.botId,
          instanceId: instance.instanceId,
          type: type,
        },
      });

      return { session };
    } catch (error) {
      this.logger.error(error);
      return;
    }
  }

  public async process(
    instance: any,
    remoteJid: string,
    bot: BotType,
    session: IntegrationSession,
    settings: SettingsType,
    content: string,
    pushName?: string,
    msg?: any,
  ): Promise<void> {
    try {
      if (!session) {
        await this.initNewSession(instance, remoteJid, bot, settings, session, content, pushName, msg);
        return;
      }

      if (session.status === 'paused') {
        return;
      }

      const keywordFinish = (settings as any)?.keywordFinish || '';
      const normalizedContent = content.toLowerCase().trim();
      if (keywordFinish.length > 0 && normalizedContent === keywordFinish.toLowerCase()) {
        await this.prismaRepository.integrationSession.update({
          where: {
            id: session.id,
          },
          data: {
            status: 'closed',
          },
        });
        return;
      }

      await this.sendMessageToBot(instance, session, settings, bot, remoteJid, pushName || '', content, msg);

      await this.prismaRepository.integrationSession.update({
        where: {
          id: session.id,
        },
        data: {
          status: 'opened',
          awaitUser: true,
        },
      });
    } catch (error) {
      this.logger.error(`Error in process: ${error}`);
      return;
    }
  }

  protected async sendMessageWhatsApp(
    instance: any,
    remoteJid: string,
    message: string,
    settings: SettingsType,
    linkPreview: boolean = true,
  ): Promise<void> {
    if (!message) return;

    const linkRegex = /!?\[(.*?)\]\((.*?)\)/g;
    let textBuffer = '';
    let lastIndex = 0;
    let match: RegExpExecArray | null;

    const splitMessages = (settings as any)?.splitMessages ?? false;

    while ((match = linkRegex.exec(message)) !== null) {
      const [fullMatch, altText, url] = match;
      const mediaType = this.getMediaType(url);
      const beforeText = message.slice(lastIndex, match.index);

      if (beforeText) {
        textBuffer += beforeText;
      }

      if (mediaType) {
        if (textBuffer.trim()) {
          await this.sendFormattedText(instance, remoteJid, textBuffer.trim(), settings, splitMessages, linkPreview);
          textBuffer = '';
        }

        try {
          if (mediaType === 'audio') {
            await instance.audioWhatsapp({
              number: remoteJid.includes('@lid') ? remoteJid : remoteJid.split('@')[0],
              delay: (settings as any)?.delayMessage || 1000,
              audio: url,
              caption: altText,
            });
          } else {
            await instance.mediaMessage(
              {
                number: remoteJid.includes('@lid') ? remoteJid : remoteJid.split('@')[0],
                delay: (settings as any)?.delayMessage || 1000,
                mediatype: mediaType,
                media: url,
                caption: altText,
                fileName: mediaType === 'document' ? altText || 'document' : undefined,
              },
              null,
              false,
            );
          }
        } catch (error) {
          this.logger.error(`Error sending media: ${error}`);
          textBuffer += `${altText}: ${url}`;
        }
      } else {
        textBuffer += fullMatch;
      }

      lastIndex = linkRegex.lastIndex;
    }

    if (lastIndex < message.length) {
      const remainingText = message.slice(lastIndex);
      if (remainingText.trim()) {
        textBuffer += remainingText;
      }
    }

    if (textBuffer.trim()) {
      await this.sendFormattedText(instance, remoteJid, textBuffer.trim(), settings, splitMessages, linkPreview);
    }
  }

  private splitMessageByDoubleLineBreaks(message: string): string[] {
    return message.split('\n\n').filter((part) => part.trim().length > 0);
  }

  private async sendSingleMessage(
    instance: any,
    remoteJid: string,
    message: string,
    settings: any,
    linkPreview: boolean = true,
  ): Promise<void> {
    const timePerChar = settings?.timePerChar ?? 0;
    const minDelay = 1000;
    const maxDelay = 20000;
    const delay = Math.min(Math.max(message.length * timePerChar, minDelay), maxDelay);

    if (instance.integration === Integration.WHATSAPP_BAILEYS) {
      await instance.client.presenceSubscribe(remoteJid);
      await instance.client.sendPresenceUpdate('composing', remoteJid);
    }

    await new Promise<void>((resolve) => {
      setTimeout(async () => {
        await instance.textMessage(
          {
            number: remoteJid.includes('@lid') ? remoteJid : remoteJid.split('@')[0],
            delay: settings?.delayMessage || 1000,
            text: message,
            linkPreview,
          },
          false,
        );
        resolve();
      }, delay);
    });

    if (instance.integration === Integration.WHATSAPP_BAILEYS) {
      await instance.client.sendPresenceUpdate('paused', remoteJid);
    }
  }

  private async sendFormattedText(
    instance: any,
    remoteJid: string,
    text: string,
    settings: any,
    splitMessages: boolean,
    linkPreview: boolean = true,
  ): Promise<void> {
    if (splitMessages) {
      const messageParts = this.splitMessageByDoubleLineBreaks(text);

      for (let index = 0; index < messageParts.length; index++) {
        const message = messageParts[index];

        await this.sendSingleMessage(instance, remoteJid, message, settings, linkPreview);
      }
    } else {
      await this.sendSingleMessage(instance, remoteJid, text, settings, linkPreview);
    }
  }

  protected async initNewSession(
    instance: any,
    remoteJid: string,
    bot: BotType,
    settings: SettingsType,
    session: IntegrationSession,
    content: string,
    pushName?: string | any,
    msg?: any,
  ): Promise<void> {
    if (!session) {
      const pushNameValue =
        typeof pushName === 'object' && pushName?.pushName
          ? pushName.pushName
          : typeof pushName === 'string'
            ? pushName
            : null;

      const sessionResult = await this.createNewSession(
        {
          instanceName: instance.instanceName,
          instanceId: instance.instanceId,
        },
        {
          remoteJid,
          pushName: pushNameValue,
          botId: (bot as any).id,
        },
        this.getBotType(),
      );

      if (!sessionResult || !sessionResult.session) {
        this.logger.error('Failed to create new session');
        return;
      }

      session = sessionResult.session;
    }

    await this.prismaRepository.integrationSession.update({
      where: {
        id: session.id,
      },
      data: {
        status: 'opened',
      },
    });

    await this.sendMessageToBot(instance, session, settings, bot, remoteJid, pushName || '', content, msg);
  }

  protected abstract getBotType(): string;

  protected abstract sendMessageToBot(
    instance: any,
    session: IntegrationSession,
    settings: SettingsType,
    bot: BotType,
    remoteJid: string,
    pushName: string,
    content: string,
    msg?: any,
  ): Promise<void>;
}
