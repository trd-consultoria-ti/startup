import { PrismaRepository } from '@api/repository/repository.service';
import { WAMonitoringService } from '@api/services/monitor.service';
import { Anthropic as AnthropicConfig, ConfigService } from '@config/env.config';
import { AnthropicBot, AnthropicSetting, IntegrationSession } from '@prisma/client';
import { sendTelemetry } from '@utils/sendTelemetry';
import { Logger } from '@config/logger.config';
import { Events } from '@api/types/wa.types';
import { createJid } from '@utils/createJid';
import cron from 'node-cron';

function sanitizePostIt(text: string): string {
  return text
    .replace(/[\u2010\u2011\u2012\u2013\u2014\u2015]/g, '-')
    .replace(/\u00A0/g, ' ')
    .replace(/\u200B/g, '')
    .split('\n')
    .map((l) => l.trimEnd())
    .join('\n')
    .trim();
}

function getDailySeed(): string {
  const days = ['dom','seg','ter','qua','qui','sex','sab'];
  const now = new Date();
  const dayName = days[now.getDay()];
  const dayNum = now.getDate();
  const weekNum = Math.ceil(dayNum / 7);
  return `${dayName}-${dayNum}-semana${weekNum}`;
}

import { BaseChatbotService } from '../../base-chatbot.service';
import { AgentOrchestrator } from '../agents/orchestrator';
import { AgentContext, OLLAMA_CONFIG, AgentTrace } from '../agents/agent-types';
import { SchedulerAgent } from '../agents/scheduler.agent';
import { RagAgent } from '../agents/rag.agent';

const DAILY_PROMPT_TARGET = process.env.DAILY_PROMPT_TARGET || '5561992078335';
const DAILY_PROMPT_CRON = process.env.DAILY_PROMPT_CRON || '0 8 * * *';
const DAILY_PROMPT_TIMEZONE = process.env.DAILY_PROMPT_TIMEZONE || 'America/Sao_Paulo';
const DAILY_PROMPT_PUSHNAME = process.env.DAILY_PROMPT_PUSHNAME || 'Thiago Duarte';

const DAILY_KNOWLEDGE_CRON = process.env.DAILY_KNOWLEDGE_CRON || '0 9 * * *';

const DAILY_API_CRON = process.env.DAILY_API_CRON || '0 10 * * *';
const DAILY_CLAUDE_CODE_CRON = process.env.DAILY_CLAUDE_CODE_CRON || '0 11 * * *';
const DAILY_JAVA_CRON = process.env.DAILY_JAVA_CRON || '0 12 * * *';
const DAILY_ANGULAR_CRON = process.env.DAILY_ANGULAR_CRON || '0 13 * * *';

export class AnthropicService extends BaseChatbotService<AnthropicBot, AnthropicSetting> {
  private readonly orchestrator: AgentOrchestrator;

  constructor(
    waMonitor: WAMonitoringService,
    prismaRepository: PrismaRepository,
    configService: ConfigService,
  ) {
    super(waMonitor, prismaRepository, 'AnthropicService', configService);
    this.orchestrator = new AgentOrchestrator(configService, prismaRepository);
    this.logger.log(
      `[AnthropicService] Orchestrator initialized | topology=hierarchical | agents=${this.orchestrator.listAgents().length} | ollama=${OLLAMA_CONFIG.baseUrl}`,
    );

    const schedulerAgent = this.orchestrator.getCoordinator().getAgent('scheduler') as SchedulerAgent | undefined;
    if (schedulerAgent) {
      schedulerAgent.setFireCallback(async (reminder) => {
        await this.fireReminderToWhatsApp(reminder);
      });
      this.logger.log('[AnthropicService] Scheduler fire callback registered');
    }

    this.sessionCleanupTimer = setInterval(() => this.cleanupOldSessions(), 60 * 60 * 1000);
    (this.sessionCleanupTimer as any).unref?.();
    this.logger.log('[AnthropicService] Session cleanup timer started (1h interval)');

    this.dailyPromptCron = cron.schedule(DAILY_PROMPT_CRON, () => {
      this.sendDailyPostIt().catch((err) => {
        this.logger.error(`[AnthropicService] Daily post-it failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily post-it cron started | cron="${DAILY_PROMPT_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);

    this.dailyKnowledgeCron = cron.schedule(DAILY_KNOWLEDGE_CRON, () => {
      this.sendDailyKnowledge().catch((err) => {
        this.logger.error(`[AnthropicService] Daily knowledge failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily knowledge cron started | cron="${DAILY_KNOWLEDGE_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);

    this.dailyApiCron = cron.schedule(DAILY_API_CRON, () => {
      this.sendDailyApiPostIt().catch((err) => {
        this.logger.error(`[AnthropicService] Daily API post-it failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily API post-it cron started | cron="${DAILY_API_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);

    this.dailyClaudeCodeCron = cron.schedule(DAILY_CLAUDE_CODE_CRON, () => {
      this.sendDailyClaudeCodePostIt().catch((err) => {
        this.logger.error(`[AnthropicService] Daily Claude Code post-it failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily Claude Code post-it cron started | cron="${DAILY_CLAUDE_CODE_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);

    this.dailyJavaCron = cron.schedule(DAILY_JAVA_CRON, () => {
      this.sendDailyJavaPostIt().catch((err) => {
        this.logger.error(`[AnthropicService] Daily Java post-it failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily Java post-it cron started | cron="${DAILY_JAVA_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);

    this.dailyAngularCron = cron.schedule(DAILY_ANGULAR_CRON, () => {
      this.sendDailyAngularPostIt().catch((err) => {
        this.logger.error(`[AnthropicService] Daily Angular post-it failed: ${err.message}`);
      });
    }, { timezone: DAILY_PROMPT_TIMEZONE });
    this.logger.log(`[AnthropicService] Daily Angular post-it cron started | cron="${DAILY_ANGULAR_CRON}" | tz=${DAILY_PROMPT_TIMEZONE} | target=${DAILY_PROMPT_TARGET}`);
  }

  private sessionCleanupTimer: NodeJS.Timeout;
  private dailyPromptCron: cron.ScheduledTask | null = null;
  private dailyKnowledgeCron: cron.ScheduledTask | null = null;
  private dailyApiCron: cron.ScheduledTask | null = null;
  private dailyClaudeCodeCron: cron.ScheduledTask | null = null;
  private dailyJavaCron: cron.ScheduledTask | null = null;
  private dailyAngularCron: cron.ScheduledTask | null = null;

  private async cleanupOldSessions(): Promise<void> {
    try {
      const settings = await this.prismaRepository.anthropicSetting.findMany();
      let cleaned = 0;

      for (const setting of settings) {
        const expireSeconds = setting.expire || 300;
        const cutoff = new Date(Date.now() - expireSeconds * 1000);

        const result = await this.prismaRepository.integrationSession.deleteMany({
          where: {
            status: 'opened',
            awaitUser: true,
            updatedAt: { lt: cutoff },
            botId: { not: null },
          },
        });
        cleaned += result.count || 0;
      }

      if (cleaned > 0) {
        this.logger.log(`[AnthropicService] Cleaned up ${cleaned} expired sessions`);
      }
    } catch (err: any) {
      this.logger.warn(`[AnthropicService] Session cleanup failed: ${err.message}`);
    }
  }

  private async fireReminderToWhatsApp(reminder: any): Promise<void> {
    try {
      const session = await this.prismaRepository.integrationSession.findFirst({
        where: { remoteJid: reminder.remoteJid },
        include: { Instance: { select: { name: true } } },
      });

      const instanceName = session?.Instance?.name;
      if (!instanceName) {
        this.logger.warn(`[AnthropicService] Could not find instance for reminder: ${reminder.remoteJid}`);
        return;
      }

      const instance = this.waMonitor.waInstances[instanceName];
      const isConnected = instance?.connectionStatus?.state === 'open';

      if (!instance || !isConnected) {
        this.logger.warn(`[AnthropicService] Instance "${instanceName}" not connected for reminder: ${reminder.remoteJid}. Scheduling retry in 5min.`);
        const schedulerAgent = this.orchestrator.getCoordinator().getAgent('scheduler') as SchedulerAgent | undefined;
        if (schedulerAgent && typeof schedulerAgent.rescheduleReminder === 'function') {
          await schedulerAgent.rescheduleReminder(reminder.id, 5 * 60 * 1000);
        }
        return;
      }

      const settings = await this.prismaRepository.anthropicSetting.findFirst({
        where: { instanceId: session.instanceId },
      });

      const message = `🔔 *Lembrete:* ${reminder.message}\n\nResponda "snooze" para adiar ou "feito" para concluir.`;
      await this.sendMessageWhatsApp(instance, reminder.remoteJid, message, (settings || {}) as any, true);
      this.logger.log(`[AnthropicService] Reminder fired to WhatsApp: ${reminder.remoteJid}`);

      try {
        instance?.sendDataWebhook?.(Events.SCHEDULER_REMINDER_FIRED, {
          reminderId: reminder.id,
          remoteJid: reminder.remoteJid,
          botId: reminder.botId,
          message: reminder.message,
          triggerAt: reminder.triggerAt,
          firedAt: reminder.firedAt,
          recurrence: reminder.recurrence || null,
        });
      } catch (webhookErr: any) {
        this.logger.warn(`[AnthropicService] Webhook dispatch failed for reminder: ${webhookErr.message}`);
      }
    } catch (error: any) {
      this.logger.error(`[AnthropicService] Failed to fire reminder: ${error.message}`);
    }
  }

  private async sendDailyPostIt(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily post-it triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily post-it');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily post-it. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const exercisePrompt = `Voce e um especialista em prompt engineering. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA.

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- NAO use topicos genericos como "Prompt Engineering" - seja especifico e tecnico
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "defina objetivo claro" ou "seja especifico" - seja original

TOPICOS POSSIVEIS (escolha baseado no seed, nunca repita):
Chain of Thought, Few-shot prompting, Zero-shot, System Prompt, Temperature, Top-p, Context Window, Token limits, XML tags estruturadas, Role prompting, Step-by-step decomposition, Negative prompting, Prompt chaining, Self-consistency, ReAct pattern, Delimiters, Output format specification, Guardrails, Prompt injection defense, Multi-turn optimization

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: exercisePrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - Prompt Engineering*\n\n`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily post-it sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily post-it: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily post-it orchestrator error: ${err.message}`);
    }
  }

  private async sendDailyKnowledge(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily knowledge triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily knowledge');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily knowledge. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const knowledgePrompt = `Voce e um instrutor certificado pela Anthropic Academy. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA sobre o curso "AI Fluency: Framework & Foundations".

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "defina objetivo claro" ou "seja especifico" - seja original

O curso cobre o 4D Framework com estes modulos especificos:
- Delegation: o que delegar vs o que nao delegar, identificar tarefas adequadas para IA, definir limites claros
- Description: como descrever tarefas com clareza, especificidade, contexto, formato de saida
- Discernment: avaliar respostas de IA, detectar alucinacoes, viés, fatos vs confabulacao
- Diligence: revisao sistematica, iteracao de prompts, documentacao de resultados, seguranca
- Generative AI fundamentals: capacidades e limitacoes, o que IA pode vs nao pode fazer
- Effective prompting: tecnicas concretas de prompting dentro do framework 4D
- Description-Discernment loop: ciclo iterativo de refinar prompts e avaliar respostas

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico do modulo]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: knowledgePrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - AI Fluency*\n\n`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily knowledge sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily knowledge: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily knowledge orchestrator error: ${err.message}`);
    }
  }

  private async sendDailyApiPostIt(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily API post-it triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily API post-it');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily API post-it. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const apiPrompt = `Voce e um engenheiro sênior da Anthropic. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA sobre o curso "Building with the Claude API".

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- NAO use topicos genericos como "Prompt Engineering" ou "API key" - seja especifico e tecnico
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "defina objetivo claro" ou "seja especifico" - seja original

TOPICOS POSSIVEIS (escolha baseado no seed, nunca repita):
- API: multi-turn conversations, system prompts, temperature, response streaming, structured data (JSON mode)
- Prompt evaluation: eval workflow, test datasets, model-based grading, code-based grading
- Prompt engineering: XML tags, being specific, providing examples, being clear and direct
- Tool use: tool schemas, handling message blocks, tool results, multi-turn with tools, multiple tools, fine-grained tool calling, text edit tool, web search tool
- RAG: text chunking strategies, text embeddings, full RAG flow, BM25 lexical search, multi-index RAG pipeline
- Features: extended thinking, image support, PDF support, citations, prompt caching (cache do prefixo do prompt para reduzir custo e latencia), code execution, Files API
- MCP: MCP clients, defining tools with MCP, server inspector, implementing a client, defining resources, accessing resources, defining prompts
- Agents: parallelization workflows, chaining workflows, routing workflows, agents vs tools, workflows vs agents, environment inspection

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico e tecnico]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: apiPrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - Claude API*

`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily API post-it sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily API post-it: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily API post-it orchestrator error: ${err.message}`);
    }
  }

  private async sendDailyClaudeCodePostIt(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily Claude Code post-it triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily Claude Code post-it');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily Claude Code post-it. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const claudeCodePrompt = `Voce e um especialista em Claude Code. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA sobre o curso "Claude Code in Action".

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "defina objetivo claro" ou "seja especifico" - seja original

TOPICOS POSSIVEIS (escolha baseado no seed, nunca repita):
- Steer the Work: steering long sessions, como guiar sessoes sem perder contexto, quando redirecionar
- Configure Claude: CLAUDE.md que funciona, verification skills, permission modes (plan, auto-edit, full-auto), hooks (pre/post tool use)
- Automate repeat work: routines, headless mode, GitHub Actions integration, code review automatizado
- Verify and share: verifying unsupervised runs, trust calibration, plugins como extender capacidades

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico e tecnico]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: claudeCodePrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - Claude Code*

`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily Claude Code post-it sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily Claude Code post-it: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily Claude Code post-it orchestrator error: ${err.message}`);
    }
  }

  private async sendDailyJavaPostIt(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily Java post-it triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily Java post-it');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily Java post-it. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const javaPrompt = `Voce e um arquiteto Java sênior. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA sobre Java e ecossistema Spring.

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "use boas praticas" ou "seja claro" - seja original

TOPICOS POSSIVEIS (escolha baseado no seed, nunca repita):
- Core Java: Records, Sealed classes, Pattern matching, Switch expressions, Virtual threads, Scoped values, Structured concurrency
- Spring Boot: Auto-configuration, Dependency Injection, Profiles, Actuator, Conditional beans, Configuration properties
- Spring Data JPA: Entity design, Repository patterns, N+1 problem, JPQL vs Criteria, Lazy vs Eager fetching, Pagination
- Spring Security: SecurityFilterChain, JWT, OAuth2, Method-level security, CSRF, Password encoding
- Spring Web: RestController, ExceptionHandler, Validation, Content negotiation, Interceptors, Filters
- Performance: JVM tuning, Garbage collection (G1, ZGC), Memory model, Thread pools, Connection pooling (HikariCP)
- Testing: JUnit 5, Mockito, Testcontainers, @WebMvcTest, @DataJpaTest, Integration tests, AssertJ
- Build: Maven vs Gradle, Dependency management, Multi-module projects, BOM
- Concorrencia: CompletableFuture, ExecutorService, ConcurrentHashMap, Synchronized vs Lock, Virtual threads

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico e tecnico]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: javaPrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - Java*

`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily Java post-it sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily Java post-it: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily Java post-it orchestrator error: ${err.message}`);
    }
  }

  private async sendDailyAngularPostIt(): Promise<void> {
    const remoteJid = createJid(DAILY_PROMPT_TARGET);
    this.logger.log(`[AnthropicService] Daily Angular post-it triggered -> ${remoteJid}`);

    const bot = await this.prismaRepository.anthropicBot.findFirst({
      where: { enabled: true },
      include: { Instance: { select: { name: true, id: true } } },
    });

    if (!bot || !bot.Instance?.name) {
      this.logger.warn('[AnthropicService] No enabled AnthropicBot found for daily Angular post-it');
      return;
    }

    const instance = this.waMonitor.waInstances[bot.Instance.name];
    if (!instance || instance?.connectionStatus?.state !== 'open') {
      this.logger.warn(`[AnthropicService] Instance "${bot.Instance.name}" not connected for daily Angular post-it. Skipping.`);
      return;
    }

    const settings = await this.prismaRepository.anthropicSetting.findFirst({
      where: { instanceId: bot.instanceId },
    });

    const angularPrompt = `Voce e um arquiteto Angular sênior. Gere um post-it diario de fixacao de conhecimento de ALTA QUALIDADE TECNICA sobre Angular moderno (v17+).

Seed de hoje: ${getDailySeed()}
Use o seed acima para escolher um topico DIFERENTE a cada dia. NUNCA gere o mesmo topico duas vezes.

REGRAS OBRIGATORIAS:
- NAO mencione skills, tools, plugins ou qualquer meta-comentario sobre o processo
- Use APENAS hifen comum (-), nunca hifens especiais unicode
- Responda APENAS com o conteudo do post-it, nada antes ou depois
- A definicao deve ser tecnicamente precisa, nao vaga
- Os 3 pontos de fixacao devem ser insights acionaveis, nao obvios
- A acao de hoje deve ser algo que o leitor pode fazer em 15 minutos
- NAO repita insights como "use boas praticas" ou "seja claro" - seja original

TOPICOS POSSIVEIS (escolha baseado no seed, nunca repita):
- Signals: signal(), computed(), effect(), linkedSignal(), resource(), writableSignal
- Change Detection: OnPush, zoneless, markForCheck, AsyncPipe vs Signals
- Components: Standalone components, lazy loading, input signals, output signals, model signals
- Dependency Injection: provideIn, InjectionToken, tree-shakable providers, hierarchical injectors
- Routing: Router outlets, guards (functional), resolvers, lazy routes, RouterLink
- Forms: Reactive forms, FormControl, FormGroup, FormArray, validators, signals forms
- RxJS: Observable, Subject, BehaviorSubject, switchMap, combineLatest, takeUntilDestroyed, marble testing
- State Management: NgRx, Signals store, provideState, component store
- Performance: TrackBy, pure pipes, defer blocks, hydration, SSR with Angular Universal
- Testing: TestBed, ComponentFixture, harness, HttpTestingController, fakeAsync
- Directives: Structural directives, attribute directives, host bindings/listeners
- Pipes: Pure vs impure, async pipe, custom pipes

Use exatamente esta estrutura (sem linhas em branco extras):

*Conceito-chave:* [topico especifico e tecnico]
*Definicao:* [definicao tecnica precisa em 1 frase - max 25 palavras]
*Fixacao:*
- [insight tecnico acionavel 1]
- [insight tecnico acionavel 2]
- [insight tecnico acionavel 3]
*Acao de hoje:* [tarefa concreta em 15 min]

Maximo 8 linhas. Seja tecnico, direto e memoravel.`;

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session: null,
      settings: settings || {},
      content: angularPrompt,
      pushName: DAILY_PROMPT_PUSHNAME,
      msg: null,
      conversationHistory: [],
      claudeTextSessionId: '',
      claudeVisionSessionId: '',
    };

    try {
      const result = await this.orchestrator.execute(agentContext);

      if (result?.finalResponse) {
        const header = `*Post-it Diario - Angular*

`;
        await this.sendMessageWhatsApp(instance, remoteJid, header + sanitizePostIt(result.finalResponse), (settings || {}) as any, true);
        this.logger.log(`[AnthropicService] Daily Angular post-it sent to ${remoteJid} | agent=${result.agentUsed} | time=${result.metadata?.processingTime}ms`);
      } else {
        this.logger.warn('[AnthropicService] Daily Angular post-it: orchestrator returned no response');
      }
    } catch (err: any) {
      this.logger.error(`[AnthropicService] Daily Angular post-it orchestrator error: ${err.message}`);
    }
  }

  protected getBotType(): string {
    return 'anthropic';
  }

  protected initClient(_apiKey?: string) {
    return null;
  }

  public async process(
    instance: any,
    remoteJid: string,
    bot: any,
    session: any,
    settings: any,
    content: string,
    pushName?: string,
    msg?: any,
  ): Promise<void> {
    this.logger.log(
      `[ARCH] >>> process() | remoteJid=${remoteJid} | botId=${bot?.id} | sessionStatus=${session?.status || 'none'} | contentLen=${content?.length || 0}`,
    );
    try {
      if (content.startsWith('audioMessage|') && msg) {
        this.logger.log('Detected audio message, attempting to transcribe');
        const audioAgent = this.orchestrator.getCoordinator().getAgent('audio');
        if (audioAgent) {
          const audioCtx: AgentContext = {
            instance, remoteJid, bot, session, settings, content, pushName, msg,
            conversationHistory: [],
          };
          const audioResult = await audioAgent.execute(audioCtx);
          if (audioResult.status === 'completed' && audioResult.response) {
            content = audioResult.response;
          } else {
            this.logger.error('Failed to transcribe audio');
            await this.sendMessageWhatsApp(
              instance, remoteJid,
              "Sorry, I couldn't transcribe your audio message. Could you please type your message instead?",
              settings, true,
            );
            return;
          }
        }
      }

      if (this.isDocumentMessage(content) && msg) {
        this.logger.log('Detected document message, attempting to extract text');
        const docAgent = this.orchestrator.getCoordinator().getAgent('document');
        if (docAgent) {
          const originalContent = content;
          const caption = originalContent.split('|').slice(2).join('|').toLowerCase().trim();
          const fileName = msg.message?.documentMessage?.fileName || msg.message?.documentWithCaption?.message?.documentMessage?.fileName || 'document';
          this.logger.log(`[RAG-DEBUG] content="${originalContent.substring(0, 80)}" | caption="${caption}" | fileName="${fileName}"`);

          const docCtx: AgentContext = {
            instance, remoteJid, bot, session, settings, content, pushName, msg,
            conversationHistory: [],
          };
          const docResult = await docAgent.execute(docCtx);
          if (docResult.status === 'completed' && docResult.response) {
            const ingestKeywords = ['ingerir', 'salvar conhecimento', 'aprender', 'memorizar', 'ingest'];
            const shouldIngest = ingestKeywords.some(kw => caption.includes(kw));
            this.logger.log(`[RAG-DEBUG] shouldIngest=${shouldIngest} | docResult.status=${docResult.status} | responseLen=${docResult.response?.length || 0}`);
            if (shouldIngest) {
              try {
                const ragAgent = this.getRagAgent();
                this.logger.log(`[RAG-DEBUG] ragAgent=${ragAgent ? 'found' : 'NOT FOUND'}`);
                if (ragAgent) {
                  const extractedText = docResult.response.replace(/^\[document:.*?\]\n/, '');
                  this.logger.log(`[RAG-DEBUG] extractedText len=${extractedText.length} | botId=${bot.id}`);
                  const ingestResult = await ragAgent.ingest(bot.id, fileName, extractedText, 'whatsapp');
                  this.logger.log(`[RAG] Ingested via WhatsApp: "${fileName}" → ${ingestResult.chunks} chunks`);
                  await this.sendMessageWhatsApp(
                    instance, remoteJid,
                    `✅ Documento "${fileName}" ingerido na base de conhecimento! (${ingestResult.chunks} chunks criados)\n\nAgora você pode me perguntar sobre o conteúdo deste documento.`,
                    settings, true,
                  );
                  return;
                }
              } catch (ingestErr: any) {
                this.logger.error(`[RAG] Ingestion failed: ${ingestErr.message}`);
                await this.sendMessageWhatsApp(
                  instance, remoteJid,
                  `❌ Erro ao ingerir documento: ${ingestErr.message}`,
                  settings, true,
                );
                return;
              }
            }
            content = docResult.response;
          } else {
            this.logger.warn('Failed to extract document text');
            await this.sendMessageWhatsApp(
              instance, remoteJid,
              'Não consegui extrair texto deste documento. Pode ser um PDF protegido, imagem escaneada ou formato não suportado (aceito: PDF, DOCX, TXT).',
              settings, true,
            );
            return;
          }
        }
      }

      const keywordFinish = settings?.keywordFinish || '';
      const normalizedContent = content.toLowerCase().trim();
      if (keywordFinish.length > 0 && normalizedContent === keywordFinish.toLowerCase()) {
        if (settings?.keepOpen) {
          await this.prismaRepository.integrationSession.update({
            where: { id: session.id },
            data: { status: 'closed' },
          });
        } else {
          await this.prismaRepository.integrationSession.delete({
            where: { id: session.id },
          });
        }
        await sendTelemetry('/anthropic/session/finish');
        return;
      }

      if (['limpar', 'limpa', 'resetar', 'reset', 'esquecer', 'esquece'].includes(normalizedContent)) {
        await this.prismaRepository.integrationSession.update({
          where: { id: session.id },
          data: {
            context: JSON.stringify({
              history: [],
              claudeTextSessionId: '',
              claudeVisionSessionId: '',
            }),
          },
        });
        this.logger.log(`[ARCH] Context cleared for ${remoteJid}`);
        await this.sendMessageWhatsApp(
          instance, remoteJid,
          '✅ Conversa limpa! Pode começar do zero.',
          settings, true,
        );
        return;
      }

      if (!session) {
        const data = { remoteJid, pushName, botId: bot.id };
        const createSession = await this.createNewSession(
          { instanceName: instance.instanceName, instanceId: instance.instanceId },
          data, this.getBotType(),
        );
        await this.initNewSession(instance, remoteJid, bot, settings, createSession.session, content, pushName, msg);
        await sendTelemetry('/anthropic/session/start');
        return;
      }

      if (session.status === 'paused') {
        await this.prismaRepository.integrationSession.update({
          where: { id: session.id },
          data: { status: 'opened', awaitUser: true },
        });
        session.status = 'opened';
      }

      await this.sendMessageToBot(instance, session, settings, bot, remoteJid, pushName || '', content, msg);
    } catch (error) {
      this.logger.error(`Error in process: ${error.message || JSON.stringify(error)}`);
      return;
    }
  }

  protected async sendMessageToBot(
    instance: any,
    session: IntegrationSession,
    settings: AnthropicSetting,
    bot: AnthropicBot,
    remoteJid: string,
    pushName: string,
    content: string,
    msg?: any,
  ): Promise<void> {
    this.logger.log(`Sending message to bot for remoteJid: ${remoteJid}`);

    try {
      const result = await this.processViaOrchestrator(instance, bot, remoteJid, session, content, pushName, msg);

      this.logger.log(
        `Orchestrator response: ${result.finalResponse?.substring(0, 50)}${result.finalResponse?.length > 50 ? '...' : ''} | agent=${result.agentUsed} | time=${result.metadata.processingTime}ms`,
      );

      if (result.finalResponse) {
        await this.sendMessageWhatsApp(instance, remoteJid, result.finalResponse, settings, true);
      } else {
        this.logger.error('No message to send to WhatsApp');
      }

      await this.prismaRepository.integrationSession.update({
        where: { id: session.id },
        data: { status: 'opened', awaitUser: true },
      });
    } catch (error) {
      this.logger.error(`Error in sendMessageToBot: ${error.message || JSON.stringify(error)}`);
      return;
    }
  }

  private async processViaOrchestrator(
    instance: any,
    bot: AnthropicBot,
    remoteJid: string,
    session: IntegrationSession,
    content: string,
    pushName: string,
    msg?: any,
  ): Promise<any> {
    let conversationHistory: any[] = [];
    let claudeTextSessionId = '';
    let claudeVisionSessionId = '';

    if (session && session.context) {
      try {
        const sessionData =
          typeof session.context === 'string' ? JSON.parse(session.context as string) : session.context;
        conversationHistory = sessionData.history || [];
        claudeTextSessionId = sessionData.claudeTextSessionId || '';
        claudeVisionSessionId = sessionData.claudeVisionSessionId || '';
      } catch (error) {
        this.logger.error(`Error parsing session context: ${error.message}`);
        conversationHistory = [];
      }
    }

    this.logger.log(
      `[ARCH] Claude CLI sessions | text=${claudeTextSessionId || 'new'} | vision=${claudeVisionSessionId || 'new'} | historyLen=${conversationHistory.length}`,
    );

    const agentContext: AgentContext = {
      instance,
      remoteJid,
      bot,
      session,
      settings: {},
      content,
      pushName,
      msg,
      conversationHistory,
      claudeTextSessionId,
      claudeVisionSessionId,
    };

    const result = await this.orchestrator.execute(agentContext);

    if (session) {
      await this.prismaRepository.integrationSession.update({
        where: { id: session.id },
        data: {
          context: JSON.stringify({
            history: result.conversationHistory,
            claudeTextSessionId: result.claudeTextSessionId || '',
            claudeVisionSessionId: result.claudeVisionSessionId || '',
          }),
        },
      });
      this.logger.log(
        `[ARCH] Session updated | sessionId=${session.id} | historyLen=${result.conversationHistory.length} | textSession=${result.claudeTextSessionId || 'none'} | visionSession=${result.claudeVisionSessionId || 'none'}`,
      );
    }

    this.logger.log(
      `[ARCH] <<< process() complete | remoteJid=${remoteJid} | agents=[${result.metadata.agentsInvolved.join(',')}] | time=${result.metadata.processingTime}ms`,
    );

    return result;
  }

  protected isImageMessage(content: string): boolean {
    return content.includes('imageMessage');
  }

  protected isDocumentMessage(content: string): boolean {
    return content.includes('documentMessage') || content.includes('documentWithCaption');
  }

  getOrchestrator(): AgentOrchestrator {
    return this.orchestrator;
  }

  getSwarmStatus() {
    return this.orchestrator.getSwarmStatus();
  }

  getRagAgent(): RagAgent | undefined {
    return this.orchestrator.getCoordinator().getRagAgent();
  }

  async ragIngest(botId: string, title: string, content: string, source?: string): Promise<{ chunks: number }> {
    const ragAgent = this.getRagAgent();
    if (!ragAgent) throw new Error('RAG agent not available');
    return await ragAgent.ingest(botId, title, content, source);
  }

  async ragListDocuments(botId: string): Promise<any[]> {
    const ragAgent = this.getRagAgent();
    if (!ragAgent) return [];
    return await ragAgent.listDocuments(botId);
  }

  async ragDeleteDocument(documentId: string): Promise<void> {
    const ragAgent = this.getRagAgent();
    if (!ragAgent) throw new Error('RAG agent not available');
    await ragAgent.deleteDocument(documentId);
  }

  getTraces(): AgentTrace[] {
    return this.orchestrator.getCoordinator().getTraces();
  }

  getAgentMetrics(): Record<string, { count: number; avgLatencyMs: number; failures: number }> {
    return this.orchestrator.getCoordinator().getAgentMetrics();
  }

  getAgentStatus(): Array<{ type: string; name: string; status: string; circuitOpen: boolean; capabilities: string[] }> {
    return this.orchestrator.getCoordinator().listAgents().map((a) => ({
      type: a.type,
      name: a.name,
      status: a.getStatus(),
      circuitOpen: a.isCircuitOpen?.() || false,
      capabilities: a.capabilities,
    }));
  }
}
