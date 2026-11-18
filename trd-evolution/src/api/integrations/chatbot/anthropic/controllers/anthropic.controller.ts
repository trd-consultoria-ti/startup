import { InstanceDto } from '@api/dto/instance.dto';
import { AnthropicCredsDto, AnthropicDto, CreateReminderDto } from '@api/integrations/chatbot/anthropic/dto/anthropic.dto';
import { AnthropicService } from '@api/integrations/chatbot/anthropic/services/anthropic.service';
import { PrismaRepository } from '@api/repository/repository.service';
import { WAMonitoringService } from '@api/services/monitor.service';
import { Anthropic as AnthropicConfig, configService } from '@config/env.config';
import { Logger } from '@config/logger.config';
import { BadRequestException } from '@exceptions';
import { IntegrationSession } from '@prisma/client';
import { createJid } from '@utils/createJid';

import { BaseChatbotController } from '../../base-chatbot.controller';

export class AnthropicController extends BaseChatbotController<any, AnthropicDto> {
  constructor(
    private readonly anthropicService: AnthropicService,
    prismaRepository: PrismaRepository,
    waMonitor: WAMonitoringService,
  ) {
    super(prismaRepository, waMonitor);

    this.botRepository = this.prismaRepository.anthropicBot;
    this.settingsRepository = this.prismaRepository.anthropicSetting;
    this.sessionRepository = this.prismaRepository.integrationSession;
    this.credsRepository = this.prismaRepository.anthropicCreds;
  }

  public readonly logger = new Logger('AnthropicController');
  protected readonly integrationName = 'Anthropic';

  integrationEnabled = configService.get<AnthropicConfig>('ANTHROPIC').ENABLED;
  botRepository: any;
  settingsRepository: any;
  sessionRepository: any;
  userMessageDebounce: { [key: string]: { message: string; timeoutId: NodeJS.Timeout } } = {};
  private credsRepository: any;

  protected getFallbackBotId(settings: any): string | undefined {
    return settings?.anthropicIdFallback;
  }

  protected getFallbackFieldName(): string {
    return 'anthropicIdFallback';
  }

  protected getIntegrationType(): string {
    return 'anthropic';
  }

  protected getAdditionalBotData(data: AnthropicDto): Record<string, any> {
    return {
      anthropicCredsId: data.anthropicCredsId,
      model: data.model,
      systemMessages: data.systemMessages,
      assistantMessages: data.assistantMessages,
      userMessages: data.userMessages,
      maxTokens: data.maxTokens,
    };
  }

  protected getAdditionalUpdateFields(data: AnthropicDto): Record<string, any> {
    return {
      anthropicCredsId: data.anthropicCredsId,
      model: data.model,
      systemMessages: data.systemMessages,
      assistantMessages: data.assistantMessages,
      userMessages: data.userMessages,
      maxTokens: data.maxTokens,
    };
  }

  protected async validateNoDuplicatesOnUpdate(botId: string, instanceId: string, data: AnthropicDto): Promise<void> {
    if (!data.model) throw new Error('Model is required');

    const whereDuplication: any = {
      id: { not: botId },
      instanceId: instanceId,
      model: data.model,
    };

    const checkDuplicate = await this.botRepository.findFirst({
      where: whereDuplication,
    });

    if (checkDuplicate) {
      throw new Error('Anthropic Bot already exists');
    }
  }

  public async createBot(instance: InstanceDto, data: AnthropicDto) {
    if (!this.integrationEnabled) throw new BadRequestException('Anthropic is disabled');

    const instanceId = await this.prismaRepository.instance
      .findFirst({
        where: {
          name: instance.instanceName,
        },
      })
      .then((instance) => instance.id);

    if (!data.model) throw new Error('Model is required');

    const whereDuplication: any = {
      instanceId: instanceId,
      model: data.model,
    };

    const checkDuplicate = await this.botRepository.findFirst({
      where: whereDuplication,
    });

    if (checkDuplicate) {
      throw new Error('Anthropic Bot already exists');
    }

    const existingSettings = await this.settingsRepository.findFirst({
      where: {
        instanceId: instanceId,
      },
    });

    if (!existingSettings) {
      await this.settings(instance, {
        anthropicCredsId: data.anthropicCredsId,
        expire: data.expire || 300,
        keywordFinish: data.keywordFinish || 'bye',
        delayMessage: data.delayMessage || 1000,
        unknownMessage: data.unknownMessage || 'Sorry, I dont understand',
        listeningFromMe: data.listeningFromMe !== undefined ? data.listeningFromMe : true,
        stopBotFromMe: data.stopBotFromMe !== undefined ? data.stopBotFromMe : true,
        keepOpen: data.keepOpen !== undefined ? data.keepOpen : false,
        debounceTime: data.debounceTime || 1,
        ignoreJids: data.ignoreJids || [],
        speechToText: false,
      });
    } else if (!existingSettings.anthropicCredsId && data.anthropicCredsId) {
      await this.settingsRepository.update({
        where: {
          id: existingSettings.id,
        },
        data: {
          AnthropicCreds: {
            connect: {
              id: data.anthropicCredsId,
            },
          },
        },
      });
    }

    return super.createBot(instance, data);
  }

  protected async processBot(
    instance: any,
    remoteJid: string,
    bot: any,
    session: IntegrationSession,
    settings: any,
    content: string,
    pushName?: string,
    msg?: any,
  ) {
    this.logger.log(`[ARCH] processBot() → anthropicService.process() | remoteJid=${remoteJid} | botModel=${bot?.model} | contentLen=${content?.length || 0}`);
    await this.anthropicService.process(instance, remoteJid, bot, session, settings, content, pushName, msg);
  }

  public async createAnthropicCreds(instance: InstanceDto, data: AnthropicCredsDto) {
    if (!this.integrationEnabled) throw new BadRequestException('Anthropic is disabled');

    const instanceId = await this.prismaRepository.instance
      .findFirst({
        where: {
          name: instance.instanceName,
        },
      })
      .then((instance) => instance.id);

    if (!data.apiKey) throw new BadRequestException('API Key is required');
    if (!data.name) throw new BadRequestException('Name is required');

    const existingApiKey = await this.credsRepository.findFirst({
      where: {
        apiKey: data.apiKey,
      },
    });

    if (existingApiKey) {
      throw new BadRequestException('This API key is already registered. Please use a different API key.');
    }

    const existingName = await this.credsRepository.findFirst({
      where: {
        name: data.name,
        instanceId: instanceId,
      },
    });

    if (existingName) {
      throw new BadRequestException('This credential name is already in use. Please choose a different name.');
    }

    try {
      const creds = await this.credsRepository.create({
        data: {
          name: data.name,
          apiKey: data.apiKey,
          instanceId: instanceId,
        },
      });

      return creds;
    } catch (error) {
      this.logger.error(error);
      throw new Error('Error creating anthropic creds');
    }
  }

  public async findAnthropicCreds(instance: InstanceDto) {
    if (!this.integrationEnabled) throw new BadRequestException('Anthropic is disabled');

    const instanceId = await this.prismaRepository.instance
      .findFirst({
        where: {
          name: instance.instanceName,
        },
      })
      .then((instance) => instance.id);

    const creds = await this.credsRepository.findMany({
      where: {
        instanceId: instanceId,
      },
    });

    return creds;
  }

  public async settings(instance: InstanceDto, data: any) {
    if (!this.integrationEnabled) throw new BadRequestException('Anthropic is disabled');

    try {
      const instanceId = await this.prismaRepository.instance
        .findFirst({
          where: {
            name: instance.instanceName,
          },
        })
        .then((instance) => instance.id);

      const existingSettings = await this.settingsRepository.findFirst({
        where: {
          instanceId: instanceId,
        },
      });

      const keywordFinish = data.keywordFinish;

      const settingsData = {
        expire: data.expire,
        keywordFinish,
        delayMessage: data.delayMessage,
        unknownMessage: data.unknownMessage,
        listeningFromMe: data.listeningFromMe,
        stopBotFromMe: data.stopBotFromMe,
        keepOpen: data.keepOpen,
        debounceTime: data.debounceTime,
        ignoreJids: data.ignoreJids,
        splitMessages: data.splitMessages,
        timePerChar: data.timePerChar,
        anthropicIdFallback: data.fallbackId,
        AnthropicCreds: data.anthropicCredsId
          ? {
              connect: {
                id: data.anthropicCredsId,
              },
            }
          : undefined,
        speechToText: data.speechToText,
      };

      if (existingSettings) {
        const settings = await this.settingsRepository.update({
          where: {
            id: existingSettings.id,
          },
          data: settingsData,
        });

        return settings;
      } else {
        const settings = await this.settingsRepository.create({
          data: {
            ...settingsData,
            Instance: {
              connect: {
                id: instanceId,
              },
            },
          },
        });

        return settings;
      }
    } catch (error) {
      this.logger.error(error);
      throw new Error('Error setting default settings');
    }
  }

  public async listReminders(instanceName: string, remoteJid?: string, botId?: string) {
    const scheduler = this.anthropicService.getOrchestrator().getCoordinator().getAgent('scheduler') as any;
    if (!scheduler) return { reminders: [] };

    if (remoteJid) {
      return { reminders: scheduler.listRemindersForUser(remoteJid, botId) };
    }
    return { reminders: scheduler.listAllReminders() };
  }

  public async getReminderStats() {
    const scheduler = this.anthropicService.getOrchestrator().getCoordinator().getAgent('scheduler') as any;
    if (!scheduler) return { total: 0, pending: 0, fired: 0, done: 0, cancelled: 0, snoozed: 0 };
    return scheduler.getReminderStats();
  }

  public async cancelReminderById(instanceName: string, reminderId: string) {
    const scheduler = this.anthropicService.getOrchestrator().getCoordinator().getAgent('scheduler') as any;
    if (!scheduler) throw new Error('Scheduler agent not found');
    const cancelled = scheduler.cancelReminderById(reminderId);
    if (!cancelled) throw new Error('Reminder not found');
    return { cancelled: true, reminderId };
  }

  public async createReminder(instance: InstanceDto, data: CreateReminderDto) {
    const scheduler = this.anthropicService.getOrchestrator().getCoordinator().getAgent('scheduler') as any;
    if (!scheduler) throw new Error('Scheduler agent not found');

    const remoteJid = data.remoteJid || (data.number ? createJid(data.number) : null);
    if (!remoteJid) throw new BadRequestException('Either "number" or "remoteJid" must be provided');

    let triggerAt: number;
    if (data.triggerAt) {
      triggerAt = new Date(data.triggerAt).getTime();
      if (isNaN(triggerAt)) throw new BadRequestException('Invalid triggerAt date. Use ISO 8601 format (e.g. 2025-09-02T10:00:00-03:00)');
    } else if (data.recurrence) {
      triggerAt = Date.now() + 60 * 1000;
    } else {
      throw new BadRequestException('Either "triggerAt" or "recurrence" must be provided');
    }

    if (triggerAt <= Date.now() && !data.recurrence) {
      throw new BadRequestException('triggerAt must be a future date');
    }

    const reminder = scheduler.createReminderProgrammatic({
      remoteJid,
      botId: data.botId,
      message: data.message,
      triggerAt,
      recurrence: data.recurrence || null,
    });

    return {
      id: reminder.id,
      remoteJid: reminder.remoteJid,
      message: reminder.message,
      triggerAt: new Date(reminder.triggerAt).toISOString(),
      recurrence: reminder.recurrence,
      status: reminder.status,
    };
  }

  public async deleteCreds(instance: InstanceDto, anthropicCredsId: string) {
    if (!this.integrationEnabled) throw new BadRequestException('Anthropic is disabled');

    const instanceId = await this.prismaRepository.instance
      .findFirst({
        where: {
          name: instance.instanceName,
        },
      })
      .then((instance) => instance.id);

    const creds = await this.credsRepository.findFirst({
      where: {
        id: anthropicCredsId,
      },
    });

    if (!creds) {
      throw new Error('Anthropic Creds not found');
    }

    if (creds.instanceId !== instanceId) {
      throw new Error('Anthropic Creds not found');
    }

    try {
      await this.credsRepository.delete({
        where: {
          id: anthropicCredsId,
        },
      });

      return { anthropicCreds: { id: anthropicCredsId } };
    } catch (error) {
      this.logger.error(error);
      throw new Error('Error deleting anthropic creds');
    }
  }

  public getOrchestrator() {
    return this.anthropicService.getOrchestrator();
  }

  public async ragIngest(instance: InstanceDto, data: { botId: string; title: string; content: string; source?: string }) {
    return await this.anthropicService.ragIngest(data.botId, data.title, data.content, data.source);
  }

  public async ragListDocuments(instance: InstanceDto, botId: string) {
    return await this.anthropicService.ragListDocuments(botId);
  }

  public async ragDeleteDocument(instance: InstanceDto, documentId: string) {
    await this.anthropicService.ragDeleteDocument(documentId);
    return { deleted: true, documentId };
  }

  public getTraces() {
    return this.anthropicService.getTraces();
  }

  public getAgentMetrics() {
    return this.anthropicService.getAgentMetrics();
  }

  public getAgentStatus() {
    return this.anthropicService.getAgentStatus();
  }
}
