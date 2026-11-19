import { authGuard } from '@api/guards/auth.guard';
import { instanceExistsGuard, instanceLoggedGuard } from '@api/guards/instance.guard';
import Telemetry from '@api/guards/telemetry.guard';
import { ChannelRouter } from '@api/integrations/channel/channel.router';
import { ChatbotRouter } from '@api/integrations/chatbot/chatbot.router';
import { EventRouter } from '@api/integrations/event/event.router';
import { StorageRouter } from '@api/integrations/storage/storage.router';
import { waMonitor, anthropicController } from '@api/server.module';
import { configService, Database, Facebook } from '@config/env.config';
import { fetchLatestWaWebVersion } from '@utils/fetchLatestWaWebVersion';
import { NextFunction, Request, Response, Router } from 'express';
import fs from 'fs';
import mimeTypes from 'mime-types';
import path from 'path';

import { BusinessRouter } from './business.router';
import { CallRouter } from './call.router';
import { ChatRouter } from './chat.router';
import { GroupRouter } from './group.router';
import { InstanceRouter } from './instance.router';
import { LabelRouter } from './label.router';
import { ProxyRouter } from './proxy.router';
import { MessageRouter } from './sendMessage.router';
import { SettingsRouter } from './settings.router';
import { TemplateRouter } from './template.router';
import { ViewsRouter } from './view.router';

enum HttpStatus {
  OK = 200,
  CREATED = 201,
  NOT_FOUND = 404,
  FORBIDDEN = 403,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  INTERNAL_SERVER_ERROR = 500,
}

const router: Router = Router();
const serverConfig = configService.get('SERVER');
const databaseConfig = configService.get<Database>('DATABASE');
const guards = [instanceExistsGuard, instanceLoggedGuard, authGuard['apikey']];

const telemetry = new Telemetry();

const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf8'));

const metricsIPWhitelist = (req: Request, res: Response, next: NextFunction) => {
  const metricsConfig = configService.get('METRICS');
  const allowedIPs = metricsConfig.ALLOWED_IPS?.split(',').map((ip) => ip.trim()) || ['127.0.0.1'];
  const clientIPs = [
    req.ip,
    req.connection.remoteAddress,
    req.socket.remoteAddress,
    req.headers['x-forwarded-for'],
  ].filter((ip) => ip !== undefined);

  if (allowedIPs.filter((ip) => clientIPs.includes(ip)) === 0) {
    return res.status(403).send('Forbidden: IP not allowed');
  }

  next();
};

const metricsBasicAuth = (req: Request, res: Response, next: NextFunction) => {
  const metricsConfig = configService.get('METRICS');
  const metricsUser = metricsConfig.USER;
  const metricsPass = metricsConfig.PASSWORD;

  if (!metricsUser || !metricsPass) {
    return res.status(500).send('Metrics authentication not configured');
  }

  const auth = req.get('Authorization');
  if (!auth || !auth.startsWith('Basic ')) {
    res.set('WWW-Authenticate', 'Basic realm="Evolution API Metrics"');
    return res.status(401).send('Authentication required');
  }

  const credentials = Buffer.from(auth.slice(6), 'base64').toString();
  const [user, pass] = credentials.split(':');

  if (user !== metricsUser || pass !== metricsPass) {
    return res.status(401).send('Invalid credentials');
  }

  next();
};

const metricsConfig = configService.get('METRICS');
if (metricsConfig.ENABLED) {
  const metricsMiddleware = [];

  if (metricsConfig.ALLOWED_IPS) {
    metricsMiddleware.push(metricsIPWhitelist);
  }

  if (metricsConfig.AUTH_REQUIRED) {
    metricsMiddleware.push(metricsBasicAuth);
  }

  router.get('/metrics', ...metricsMiddleware, async (req, res) => {
    res.set('Content-Type', 'text/plain; version=0.0.4; charset=utf-8');
    res.set('Cache-Control', 'no-cache, no-store, must-revalidate');

    const escapeLabel = (value: unknown) =>
      String(value ?? '')
        .replace(/\\/g, '\\\\')
        .replace(/\n/g, '\\n')
        .replace(/"/g, '\\"');

    const lines: string[] = [];

    const clientName = databaseConfig.CONNECTION.CLIENT_NAME || 'unknown';
    const serverUrl = serverConfig.URL || '';

    lines.push('# HELP evolution_environment_info Environment information');
    lines.push('# TYPE evolution_environment_info gauge');
    lines.push(
      `evolution_environment_info{version="${escapeLabel(packageJson.version)}",clientName="${escapeLabel(
        clientName,
      )}",serverUrl="${escapeLabel(serverUrl)}"} 1`,
    );

    const instances = (waMonitor && waMonitor.waInstances) || {};
    const instanceEntries = Object.entries(instances);

    lines.push('# HELP evolution_instances_total Total number of instances');
    lines.push('# TYPE evolution_instances_total gauge');
    lines.push(`evolution_instances_total ${instanceEntries.length}`);

    lines.push('# HELP evolution_instance_up 1 if instance state is open, else 0');
    lines.push('# TYPE evolution_instance_up gauge');
    lines.push('# HELP evolution_instance_state Instance state as a labelled metric');
    lines.push('# TYPE evolution_instance_state gauge');

    for (const [name, instance] of instanceEntries) {
      const state = instance?.connectionStatus?.state || 'unknown';
      const integration = instance?.integration || '';
      const up = state === 'open' ? 1 : 0;

      lines.push(
        `evolution_instance_up{instance="${escapeLabel(name)}",integration="${escapeLabel(integration)}"} ${up}`,
      );
      lines.push(
        `evolution_instance_state{instance="${escapeLabel(name)}",integration="${escapeLabel(
          integration,
        )}",state="${escapeLabel(state)}"} 1`,
      );
    }

    try {
      const orchestrator = anthropicController?.getOrchestrator?.();
      if (orchestrator) {
        const agentMetrics = orchestrator.getAgentMetrics();
        lines.push('# HELP evolution_agent_messages_total Total messages processed by agent type');
        lines.push('# TYPE evolution_agent_messages_total counter');
        lines.push('# HELP evolution_agent_processing_time_ms_total Total processing time in ms by agent type');
        lines.push('# TYPE evolution_agent_processing_time_ms_total counter');
        lines.push('# HELP evolution_agent_errors_total Total errors by agent type');
        lines.push('# TYPE evolution_agent_errors_total counter');

        for (const [agentType, metrics] of Object.entries(agentMetrics) as [string, { count: number; totalTimeMs: number; errors: number; avgTimeMs: number }][]) {
          lines.push(`evolution_agent_messages_total{agent="${escapeLabel(agentType)}"} ${metrics.count}`);
          lines.push(`evolution_agent_processing_time_ms_total{agent="${escapeLabel(agentType)}"} ${metrics.totalTimeMs}`);
          lines.push(`evolution_agent_errors_total{agent="${escapeLabel(agentType)}"} ${metrics.errors}`);
        }

        const scheduler = orchestrator.getCoordinator().getAgent('scheduler') as any;
        if (scheduler && typeof scheduler.getReminderStats === 'function') {
          const stats = scheduler.getReminderStats();
          lines.push('# HELP evolution_scheduler_reminders_total Total reminders by status');
          lines.push('# TYPE evolution_scheduler_reminders_total gauge');
          lines.push(`evolution_scheduler_reminders_total{status="pending"} ${stats.pending}`);
          lines.push(`evolution_scheduler_reminders_total{status="fired"} ${stats.fired}`);
          lines.push(`evolution_scheduler_reminders_total{status="done"} ${stats.done}`);
          lines.push(`evolution_scheduler_reminders_total{status="cancelled"} ${stats.cancelled}`);
          lines.push(`evolution_scheduler_reminders_total{status="snoozed"} ${stats.snoozed}`);
          lines.push(`evolution_scheduler_reminders_total{status="total"} ${stats.total}`);
        }
      }
    } catch {
      // Agent metrics optional - ignore if not available
    }

    res.send(lines.join('\n') + '\n');
  });
}

if (!serverConfig.DISABLE_MANAGER) router.use('/manager', new ViewsRouter().router);

router.get('/assets/*', (req, res) => {
  const fileName = req.params[0];

  if (!fileName || fileName.includes('..') || fileName.includes('\\') || path.isAbsolute(fileName)) {
    return res.status(403).send('Forbidden');
  }

  const basePath = path.join(process.cwd(), 'manager', 'dist');
  const assetsPath = path.join(basePath, 'assets');
  const filePath = path.join(assetsPath, fileName);

  const resolvedPath = path.resolve(filePath);
  const resolvedAssetsPath = path.resolve(assetsPath);

  if (!resolvedPath.startsWith(resolvedAssetsPath + path.sep) && resolvedPath !== resolvedAssetsPath) {
    return res.status(403).send('Forbidden');
  }

  if (fs.existsSync(resolvedPath)) {
    res.set('Content-Type', mimeTypes.lookup(resolvedPath) || 'text/css');
    res.send(fs.readFileSync(resolvedPath));
  } else {
    res.status(404).send('File not found');
  }
});

router
  .use((req, res, next) => telemetry.collectTelemetry(req, res, next))

  .get('/', async (req, res) => {
    res.status(HttpStatus.OK).json({
      status: HttpStatus.OK,
      message: 'Welcome to the Evolution API, it is working!',
      version: packageJson.version,
      clientName: databaseConfig.CONNECTION.CLIENT_NAME,
      manager: !serverConfig.DISABLE_MANAGER ? `${req.protocol}://${req.get('host')}/manager` : undefined,
      documentation: `https://doc.evolution-api.com`,
      whatsappWebVersion: (await fetchLatestWaWebVersion({})).version.join('.'),
    });
  })
  .post('/verify-creds', authGuard['apikey'], async (req, res) => {
    const facebookConfig = configService.get<Facebook>('FACEBOOK');
    return res.status(HttpStatus.OK).json({
      status: HttpStatus.OK,
      message: 'Credentials are valid',
      facebookAppId: facebookConfig.APP_ID,
      facebookConfigId: facebookConfig.CONFIG_ID,
      facebookUserToken: facebookConfig.USER_TOKEN,
    });
  })
  .post('/trd/simulate-chat', async (req, res) => {
    try {
      const { content, pushName, remoteJid, history } = req.body || {};
      if (!content) {
        return res.status(400).json({ error: 'Field "content" is required.' });
      }

      const { CoordinatorAgent } = await import('@api/integrations/chatbot/anthropic/agents/coordinator.agent');
      const { PrismaRepository } = await import('@api/repository/repository.service');
      const repo = new PrismaRepository(configService);
      const coordinator = new CoordinatorAgent(configService, repo);

      const startTime = Date.now();
      const result = await coordinator.route({
        instance: { name: 'trd-whatsapp-principal' },
        remoteJid: remoteJid || '5561999999999@s.whatsapp.com',
        bot: { id: 'trd-bot-main' },
        session: {},
        settings: {},
        content,
        pushName: pushName || 'Cliente Visitante',
        conversationHistory: Array.isArray(history) ? history : [],
      });

      return res.status(200).json({
        status: 200,
        response: result.finalResponse,
        agentUsed: result.agentUsed,
        intent: result.intent,
        latencyMs: Date.now() - startTime,
        metadata: result.metadata,
        conversationHistory: result.conversationHistory,
      });
    } catch (err: any) {
      return res.status(500).json({ error: err.message });
    }
  })
  .use('/instance', new InstanceRouter(configService, ...guards).router)
  .use('/message', new MessageRouter(...guards).router)
  .use('/call', new CallRouter(...guards).router)
  .use('/chat', new ChatRouter(...guards).router)
  .use('/business', new BusinessRouter(...guards).router)
  .use('/group', new GroupRouter(...guards).router)
  .use('/template', new TemplateRouter(configService, ...guards).router)
  .use('/settings', new SettingsRouter(...guards).router)
  .use('/proxy', new ProxyRouter(...guards).router)
  .use('/label', new LabelRouter(...guards).router)
  .use('', new ChannelRouter(configService, ...guards).router)
  .use('', new EventRouter(configService, ...guards).router)
  .use('', new ChatbotRouter(...guards).router)
  .use('', new StorageRouter(...guards).router);

export { HttpStatus, router };
