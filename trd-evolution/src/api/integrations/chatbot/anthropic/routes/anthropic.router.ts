import { RouterBroker } from '@api/abstract/abstract.router';
import { IgnoreJidDto } from '@api/dto/chatbot.dto';
import { InstanceDto } from '@api/dto/instance.dto';
import { AnthropicCredsDto, AnthropicDto, AnthropicSettingDto, CreateReminderDto } from '@api/integrations/chatbot/anthropic/dto/anthropic.dto';
import { HttpStatus } from '@api/routes/index.router';
import { anthropicController } from '@api/server.module';
import {
  anthropicCredsSchema,
  anthropicIgnoreJidSchema,
  anthropicReminderSchema,
  anthropicSchema,
  anthropicSettingSchema,
  anthropicStatusSchema,
  instanceSchema,
} from '@validate/validate.schema';
import { RequestHandler, Router } from 'express';

export class AnthropicRouter extends RouterBroker {
  constructor(...guards: RequestHandler[]) {
    super();
    this.router
      .post(this.routerPath('creds'), ...guards, async (req, res) => {
        const response = await this.dataValidate<AnthropicCredsDto>({
          request: req,
          schema: anthropicCredsSchema,
          ClassRef: AnthropicCredsDto,
          execute: (instance, data) => anthropicController.createAnthropicCreds(instance, data),
        });

        res.status(HttpStatus.CREATED).json(response);
      })
      .get(this.routerPath('creds'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.findAnthropicCreds(instance),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .delete(this.routerPath('creds/:anthropicCredsId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.deleteCreds(instance, req.params.anthropicCredsId),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .post(this.routerPath('create'), ...guards, async (req, res) => {
        const response = await this.dataValidate<AnthropicDto>({
          request: req,
          schema: anthropicSchema,
          ClassRef: AnthropicDto,
          execute: (instance, data) => anthropicController.createBot(instance, data),
        });

        res.status(HttpStatus.CREATED).json(response);
      })
      .get(this.routerPath('find'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.findBot(instance),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('fetch/:anthropicBotId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.fetchBot(instance, req.params.anthropicBotId),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .put(this.routerPath('update/:anthropicBotId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<AnthropicDto>({
          request: req,
          schema: anthropicSchema,
          ClassRef: AnthropicDto,
          execute: (instance, data) => anthropicController.updateBot(instance, req.params.anthropicBotId, data),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .delete(this.routerPath('delete/:anthropicBotId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.deleteBot(instance, req.params.anthropicBotId),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .post(this.routerPath('settings'), ...guards, async (req, res) => {
        const response = await this.dataValidate<AnthropicSettingDto>({
          request: req,
          schema: anthropicSettingSchema,
          ClassRef: AnthropicSettingDto,
          execute: (instance, data) => anthropicController.settings(instance, data),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('fetchSettings'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.fetchSettings(instance),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .post(this.routerPath('changeStatus'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: anthropicStatusSchema,
          ClassRef: InstanceDto,
          execute: (instance, data) => anthropicController.changeStatus(instance, data),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('fetchSessions/:anthropicBotId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.fetchSessions(instance, req.params.anthropicBotId),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .post(this.routerPath('ignoreJid'), ...guards, async (req, res) => {
        const response = await this.dataValidate<IgnoreJidDto>({
          request: req,
          schema: anthropicIgnoreJidSchema,
          ClassRef: IgnoreJidDto,
          execute: (instance, data) => anthropicController.ignoreJid(instance, data),
        });

        res.status(HttpStatus.OK).json(response);
      })
      .post(this.routerPath('reminders/create'), ...guards, async (req, res) => {
        const response = await this.dataValidate<CreateReminderDto>({
          request: req,
          schema: anthropicReminderSchema,
          ClassRef: CreateReminderDto,
          execute: (instance, data) => anthropicController.createReminder(instance, data),
        });

        res.status(HttpStatus.CREATED).json(response);
      })
      .get(this.routerPath('reminders'), ...guards, async (req, res) => {
        const response = await anthropicController.listReminders(req.params.instanceName, req.query.remoteJid as string, req.query.botId as string);
        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('reminders/stats'), ...guards, async (req, res) => {
        const response = await anthropicController.getReminderStats();
        res.status(HttpStatus.OK).json(response);
      })
      .delete(this.routerPath('reminders/:reminderId'), ...guards, async (req, res) => {
        const response = await anthropicController.cancelReminderById(req.params.instanceName, req.params.reminderId);
        res.status(HttpStatus.OK).json(response);
      })
      // RAG - Knowledge base management
      .post(this.routerPath('rag/ingest'), ...guards, async (req, res) => {
        const response = await this.dataValidate({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance, data) => anthropicController.ragIngest(instance, data as any),
        });
        res.status(HttpStatus.CREATED).json(response);
      })
      .get(this.routerPath('rag/documents'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.ragListDocuments(instance, req.query.botId as string),
        });
        res.status(HttpStatus.OK).json(response);
      })
      .delete(this.routerPath('rag/documents/:documentId'), ...guards, async (req, res) => {
        const response = await this.dataValidate<InstanceDto>({
          request: req,
          schema: instanceSchema,
          ClassRef: InstanceDto,
          execute: (instance) => anthropicController.ragDeleteDocument(instance, req.params.documentId),
        });
        res.status(HttpStatus.OK).json(response);
      })
      // Agent observability
      .get(this.routerPath('traces'), ...guards, async (req, res) => {
        const response = anthropicController.getTraces();
        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('metrics'), ...guards, async (req, res) => {
        const response = anthropicController.getAgentMetrics();
        res.status(HttpStatus.OK).json(response);
      })
      .get(this.routerPath('agents/status'), ...guards, async (req, res) => {
        const response = anthropicController.getAgentStatus();
        res.status(HttpStatus.OK).json(response);
      });
  }

  public readonly router: Router = Router();
}
