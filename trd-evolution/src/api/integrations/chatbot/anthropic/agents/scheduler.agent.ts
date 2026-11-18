import { AgentContext, AgentResult, SCHEDULER_CONFIG } from './agent-types';
import { BaseAgent } from './base.agent';
import cron from 'node-cron';
import Fuse from 'fuse.js';

export interface Reminder {
  id: string;
  remoteJid: string;
  botId: string;
  message: string;
  triggerAt: number;
  recurrence: string | null;
  status: 'pending' | 'fired' | 'done' | 'cancelled' | 'snoozed';
  snoozeCount: number;
  createdAt: number;
  firedAt: number | null;
}

type ReminderCallback = (reminder: Reminder) => void;

const TIME_KEYWORDS: Record<string, number> = {
  'segundo': 1000,
  'segundos': 1000,
  'minuto': 60 * 1000,
  'minutos': 60 * 1000,
  'hora': 60 * 60 * 1000,
  'horas': 60 * 60 * 1000,
  'h': 60 * 60 * 1000,
  'dia': 24 * 60 * 60 * 1000,
  'dias': 24 * 60 * 60 * 1000,
  'semana': 7 * 24 * 60 * 60 * 1000,
  'semanas': 7 * 24 * 60 * 60 * 1000,
};

const WEEKDAYS: Record<string, number> = {
  'domingo': 0,
  'dom': 0,
  'segunda': 1,
  'seg': 1,
  'segunda-feira': 1,
  'terca': 2,
  'terça': 2,
  'ter': 2,
  'quarta': 3,
  'qua': 3,
  'quinta': 4,
  'qui': 4,
  'sexta': 5,
  'sex': 5,
  'sabado': 6,
  'sábado': 6,
  'sab': 6,
  'sáb': 6,
};

const DONE_VERBS = ['feito', 'feita', 'pronto', 'pronta', 'pago', 'paguei', 'pagou', 'concluido', 'concluída', 'resolvido', 'resolvida', 'ok', 'done', 'complete'];
const CANCEL_VERBS = ['cancela', 'cancelar', 'cancelado', 'cancelada', 'remove', 'remover', 'deleta', 'deletar', 'apagar', 'apague', 'apaga', 'limpar', 'limpe', 'limpa'];
const LIST_VERBS = ['meus lembretes', 'listar lembretes', 'lista de lembretes', 'quais lembretes', 'lembretes pendentes', 'ver lembretes'];
const SNOOZE_VERBS = ['snooze', 'adiar', 'depois', 'mais tarde'];

const CREATION_KEYWORDS = ['lembre', 'lembreme', 'lembrete', 'lembra', 'avise', 'avisa', 'agenda', 'agendar', 'alarme'];
const REMINDER_KEYWORDS = ['lembre', 'lembreme', 'lembrete', 'lembra', 'lembr', 'avise', 'avisa', 'aviza', 'agenda', 'agendar', 'alarme'];

const fuseCreation = new Fuse(CREATION_KEYWORDS, { threshold: 0.4, minMatchCharLength: 3, includeScore: true });
const fuseReminder = new Fuse(REMINDER_KEYWORDS, { threshold: 0.5, minMatchCharLength: 3, includeScore: true });
const fuseDone = new Fuse(DONE_VERBS, { threshold: 0.3, minMatchCharLength: 3, includeScore: true });
const fuseList = new Fuse(LIST_VERBS, { threshold: 0.4, minMatchCharLength: 5, includeScore: true });
const fuseCancel = new Fuse(CANCEL_VERBS, { threshold: 0.3, minMatchCharLength: 4, includeScore: true });
const fuseSnooze = new Fuse(SNOOZE_VERBS, { threshold: 0.4, minMatchCharLength: 4, includeScore: true });

function fuzzyStartsWith(content: string, fuse: Fuse<string>, threshold: number = 0.4): boolean {
  const firstWord = content.split(/\s+/)[0];
  if (firstWord.length < 3) return false;
  return fuse.search(firstWord).some((r) => (r.score ?? 1) <= threshold);
}

function fuzzyIncludes(content: string, fuse: Fuse<string>, threshold: number = 0.4): boolean {
  const words = content.split(/\s+/);
  for (const word of words) {
    if (word.length < 3) continue;
    if (fuse.search(word).some((r) => (r.score ?? 1) <= threshold)) return true;
  }
  return false;
}

export class SchedulerAgent extends BaseAgent {
  private readonly reminders = new Map<string, Reminder>();
  private readonly cronJobs = new Map<string, cron.ScheduledTask>();
  private pollTimer: NodeJS.Timeout | null = null;
  private onFireCallback: ReminderCallback | null = null;
  private prisma: any = null;

  constructor(prismaRepository?: any) {
    super({
      type: 'scheduler',
      name: 'Scheduler Agent',
      capabilities: ['reminders', 'scheduling', 'cron', 'natural-language-time'],
    });
    this.prisma = prismaRepository;
    this.startPoll();
    this.loadFromDb().catch((err) => {
      this.logger.warn(`[SchedulerAgent] Failed to load reminders from DB: ${err.message}`);
    });
  }

  setFireCallback(cb: ReminderCallback): void {
    this.onFireCallback = cb;
  }

  canHandle(context: AgentContext): boolean {
    const rawContent = context.content || '';

    if (rawContent.startsWith('[document:')) return false;

    const content = this.cleanContent(rawContent).toLowerCase().trim();

    const hasReminderKeyword = fuzzyIncludes(content, fuseReminder)
      || ['lembrete', 'lembre', 'lembreme', 'avise', 'avisa', 'agenda', 'agendar', 'alarme'].some((kw) => content.includes(kw));

    if (!hasReminderKeyword) return false;

    if (fuzzyStartsWith(content, fuseSnooze) || content.startsWith('snooze')) return true;
    if (fuzzyIncludes(content, fuseCancel) && (content.includes('lembrete') || content.includes('todos') || content.includes('tudo'))) return true;
    if (LIST_VERBS.some((v) => content.includes(v))) return true;

    const isCreation = fuzzyStartsWith(content, fuseCreation);

    if (!isCreation && fuzzyIncludes(content, fuseDone)) {
      return true;
    }

    return this.hasTimeReference(content);
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const rawContent = this.cleanContent(context.content);
      const firstLine = rawContent.split(/\n+/)[0].trim();
      const content = firstLine.toLowerCase().trim();
      const remoteJid = context.remoteJid;
      const botId = context.bot?.id || 'unknown';

      if (LIST_VERBS.some((v) => content.includes(v))) {
        return this.handleListReminders(remoteJid, botId, startTime);
      }

      if (fuzzyIncludes(content, fuseCancel) && (content.includes('lembrete') || content.includes('todos') || content.includes('tudo'))) {
        if (content.includes('todos') || content.includes('tudo')) {
          return this.handleCancelAll(remoteJid, botId, startTime);
        }
        return this.handleCancelReminder(remoteJid, botId, firstLine, startTime);
      }

      if (fuzzyStartsWith(content, fuseSnooze) || content.startsWith('snooze') || SNOOZE_VERBS.some((v) => content.includes(v))) {
        const snoozeMinutes = this.parseSnoozeMinutes(content);
        return this.handleSnooze(remoteJid, botId, snoozeMinutes, startTime);
      }

      const isCreation = fuzzyStartsWith(content, fuseCreation);

      if (!isCreation && (DONE_VERBS.some((v) => content.trim() === v) || fuzzyIncludes(content, fuseDone))) {
        const done = this.markDone(remoteJid, botId);
        if (done) {
          return this.success('✅ Lembrete concluído!', undefined, {
            processingTime: Date.now() - startTime,
          });
        }
      }

      return this.handleCreateReminder(remoteJid, botId, firstLine, content, startTime);
    } catch (error: any) {
      return this.failure(error.message || 'Scheduler agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  private handleCreateReminder(
    remoteJid: string,
    botId: string,
    rawContent: string,
    content: string,
    startTime: number,
  ): AgentResult {
    const userReminders = this.getUserReminders(remoteJid, botId);
    if (userReminders.length >= SCHEDULER_CONFIG.maxRemindersPerUser) {
      return this.failure(
        `Você já tem ${SCHEDULER_CONFIG.maxRemindersPerUser} lembretes ativos. Cancele alguns antes de criar novos.`,
        { processingTime: Date.now() - startTime },
      );
    }

    const parsed = this.parseReminder(content);
    if (!parsed) {
      return this.failure(
        'Não consegui entender quando você quer ser lembrado. Tente algo como: "lembre de pagar a conta amanhã 10h" ou "me avise em 2 horas sobre a reunião".',
        { processingTime: Date.now() - startTime },
      );
    }

    const reminderMessage = this.extractReminderMessage(rawContent, parsed);
    const triggerAt = parsed.triggerAt;
    const now = Date.now();

    if (triggerAt <= now) {
      return this.failure(
        'O horário que você pediu já passou. Tente um horário futuro.',
        { processingTime: Date.now() - startTime },
      );
    }

    const reminder: Reminder = {
      id: this.generateId(),
      remoteJid,
      botId,
      message: reminderMessage,
      triggerAt,
      recurrence: parsed.recurrence || null,
      status: 'pending',
      snoozeCount: 0,
      createdAt: now,
      firedAt: null,
    };

    this.reminders.set(reminder.id, reminder);
    this.scheduleReminder(reminder);
    this.saveReminderToDb(reminder);

    const dateStr = this.formatDate(triggerAt);
    const recurrenceStr = reminder.recurrence ? ` (recorrente: ${reminder.recurrence})` : '';

    this.logger.log(`[SchedulerAgent] Created reminder: "${reminderMessage}" for ${remoteJid} at ${dateStr}`);

    return this.success(
      `✅ Lembrete criado: *${reminderMessage}*\n⏰ ${dateStr}${recurrenceStr}\n\nResponda "snooze" para adiar ou "feito" para concluir quando disparar.`,
      undefined,
      {
        reminderId: reminder.id,
        triggerAt,
        recurrence: reminder.recurrence,
        processingTime: Date.now() - startTime,
      },
    );
  }

  private handleListReminders(remoteJid: string, botId: string, startTime: number): AgentResult {
    const reminders = this.getUserReminders(remoteJid, botId).filter((r) => r.status === 'pending' || r.status === 'snoozed');

    if (reminders.length === 0) {
      return this.success('Você não tem lembretes pendentes.', undefined, {
        processingTime: Date.now() - startTime,
      });
    }

    const lines = reminders.map((r, i) => {
      const dateStr = this.formatDate(r.triggerAt);
      const recStr = r.recurrence ? ` (recorrente)` : '';
      return `${i + 1}. *${r.message}* — ${dateStr}${recStr}`;
    });

    return this.success(
      `📋 *Seus lembretes (${reminders.length}):*\n\n${lines.join('\n')}\n\nResponda "cancela lembrete N" para remover.`,
      undefined,
      { count: reminders.length, processingTime: Date.now() - startTime },
    );
  }

  private handleCancelReminder(remoteJid: string, botId: string, rawContent: string, startTime: number): AgentResult {
    const content = rawContent.toLowerCase().trim();
    const userReminders = this.getUserReminders(remoteJid, botId).filter((r) => r.status === 'pending' || r.status === 'snoozed');

    if (userReminders.length === 0) {
      return this.success('Você não tem lembretes para cancelar.', undefined, {
        processingTime: Date.now() - startTime,
      });
    }

    const numMatch = content.match(/(\d+)/);
    if (numMatch) {
      const idx = parseInt(numMatch[1], 10) - 1;
      if (idx >= 0 && idx < userReminders.length) {
        const reminder = userReminders[idx];
        this.cancelReminder(reminder.id);
        this.logger.log(`[SchedulerAgent] Cancelled reminder #${idx + 1}: "${reminder.message}"`);
        return this.success(`✅ Lembrete cancelado: *${reminder.message}*`, undefined, {
          reminderId: reminder.id,
          processingTime: Date.now() - startTime,
        });
      }
    }

    const keyword = content.replace(/cancel[ae]r?\s*lembrete?\s*/g, '').replace(/do\s+/g, '').replace(/da\s+/g, '').trim();
    if (keyword.length > 2) {
      const match = userReminders.find((r) => r.message.toLowerCase().includes(keyword));
      if (match) {
        this.cancelReminder(match.id);
        return this.success(`✅ Lembrete cancelado: *${match.message}*`, undefined, {
          reminderId: match.id,
          processingTime: Date.now() - startTime,
        });
      }
    }

    return this.failure(
      'Não encontrei qual lembrete cancelar. Use "cancela lembrete N" onde N é o número da lista, ou "cancela lembrente <palavra-chave>".',
      { processingTime: Date.now() - startTime },
    );
  }

  private handleCancelAll(remoteJid: string, botId: string, startTime: number): AgentResult {
    const userReminders = this.getUserReminders(remoteJid, botId).filter(
      (r) => r.status === 'pending' || r.status === 'snoozed' || r.status === 'fired',
    );

    if (userReminders.length === 0) {
      return this.success('Você não tem lembretes para cancelar.', undefined, {
        processingTime: Date.now() - startTime,
      });
    }

    let count = 0;
    for (const reminder of userReminders) {
      this.cancelReminder(reminder.id);
      count++;
    }

    this.logger.log(`[SchedulerAgent] Cancelled all ${count} reminders for ${remoteJid}`);
    return this.success(`✅ ${count} lembrete(s) cancelado(s)!`, undefined, {
      processingTime: Date.now() - startTime,
    });
  }

  private handleSnooze(remoteJid: string, botId: string, snoozeMinutes: number, startTime: number): AgentResult {
    const userReminders = this.getUserReminders(remoteJid, botId).filter((r) => r.status === 'fired' || r.status === 'snoozed');

    if (userReminders.length === 0) {
      return this.failure('Não há lembretes disparados para adiar agora.', {
        processingTime: Date.now() - startTime,
      });
    }

    const minutes = snoozeMinutes || SCHEDULER_CONFIG.snoozeMinutes;
    const reminder = userReminders[0];

    if (reminder.snoozeCount >= SCHEDULER_CONFIG.maxSnoozeCount) {
      return this.failure(
        `Você já adiou este lembrete ${SCHEDULER_CONFIG.maxSnoozeCount} vezes. Conclua com "feito" ou cancele com "cancela lembrete".`,
        { processingTime: Date.now() - startTime },
      );
    }

    reminder.status = 'snoozed';
    reminder.snoozeCount++;
    reminder.triggerAt = Date.now() + minutes * 60 * 1000;

    this.cancelCronJob(reminder.id);
    this.scheduleReminder(reminder);
    this.updateReminderInDb(reminder);

    this.logger.log(`[SchedulerAgent] Snoozed reminder "${reminder.message}" by ${minutes}min (count: ${reminder.snoozeCount})`);

    return this.success(
      `⏰ Adiado por ${minutes} minutos. Lembrarei às ${this.formatDate(reminder.triggerAt)}.`,
      undefined,
      { reminderId: reminder.id, snoozeMinutes: minutes, processingTime: Date.now() - startTime },
    );
  }

  private parseReminder(content: string): { triggerAt: number; recurrence: string | null } | null {
    const now = new Date();

    const absoluteMatch = this.parseAbsoluteTime(content, now);
    if (absoluteMatch) return absoluteMatch;

    const relativeMatch = this.parseRelativeTime(content, now);
    if (relativeMatch) return relativeMatch;

    const recurringMatch = this.parseRecurring(content, now);
    if (recurringMatch) return recurringMatch;

    return null;
  }

  private parseRelativeTime(content: string, now: Date): { triggerAt: number; recurrence: string | null } | null {
    const dayKeywords = ['hoje', 'amanhã', 'amanha', 'depois de amanhã', 'depois de amanha', 'próxima semana', 'proxima semana'];
    if (dayKeywords.some((kw) => content.includes(kw))) return null;

    const weekdayInContent = Object.keys(WEEKDAYS).some((day) => {
      if (day.length <= 3) {
        return new RegExp(`\b${day}\b`, 'i').test(content);
      }
      return content.includes(` ${day}`) || content.includes(`${day} `);
    });
    if (weekdayInContent) return null;

    const patterns = [
      /(?:em\s+)(\d+)\s*(segundo[s]?|minuto[s]?|hora[s]?|h|dia[s]?|semana[s]?)/i,
      /(?:daqui\s+(?:a\s+)?)(\d+)\s*(segundo[s]?|minuto[s]?|hora[s]?|h|dia[s]?|semana[s]?)/i,
    ];

    for (const pattern of patterns) {
      const match = content.match(pattern);
      if (match) {
        const value = parseInt(match[1], 10);
        const unit = match[2].toLowerCase();
        const multiplier = TIME_KEYWORDS[unit];
        if (multiplier) {
          return { triggerAt: now.getTime() + value * multiplier, recurrence: null };
        }
      }
    }

    if (content.includes('agora') || content.includes('já')) {
      return { triggerAt: now.getTime() + 60 * 1000, recurrence: null };
    }

    return null;
  }

  private parseAbsoluteTime(content: string, now: Date): { triggerAt: number; recurrence: string | null } | null {
    let targetDate = new Date(now);

    if (content.includes('hoje')) {
      targetDate = new Date(now);
    } else if (content.includes('amanhã') || content.includes('amanha')) {
      targetDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
    } else if (content.includes('depois de amanhã') || content.includes('depois de amanha')) {
      targetDate = new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000);
    } else if (content.includes('próxima semana') || content.includes('proxima semana')) {
      targetDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    } else {
      for (const [dayName, dayNum] of Object.entries(WEEKDAYS)) {
        if (content.includes(`próximo ${dayName}`) || content.includes(`proximo ${dayName}`) || content.includes(`próxima ${dayName}`) || content.includes(`proxima ${dayName}`)) {
          const diff = (dayNum - now.getDay() + 7) % 7 || 7;
          targetDate = new Date(now.getTime() + diff * 24 * 60 * 60 * 1000);
          break;
        }
        if (dayName.length <= 3) {
          if (new RegExp(`\\b${dayName}\\b`, 'i').test(content)) {
            const diff = (dayNum - now.getDay() + 7) % 7;
            if (diff > 0) {
              targetDate = new Date(now.getTime() + diff * 24 * 60 * 60 * 1000);
              break;
            }
          }
        } else if (content.includes(` ${dayName}`) || content.includes(`${dayName} `)) {
          const diff = (dayNum - now.getDay() + 7) % 7;
          if (diff > 0) {
            targetDate = new Date(now.getTime() + diff * 24 * 60 * 60 * 1000);
            break;
          }
        }
      }

      const dateMatch = content.match(/(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{2,4}))?/);
      if (dateMatch) {
        const day = parseInt(dateMatch[1], 10);
        const month = parseInt(dateMatch[2], 10) - 1;
        let year = dateMatch[3] ? parseInt(dateMatch[3], 10) : now.getFullYear();
        if (year < 100) year += 2000;
        targetDate = new Date(year, month, day);
      }
    }

    const timeMatch = content.match(/(\d{1,2})[:h](\d{2})?\s*(h|horas)?/i);
    if (timeMatch) {
      const hours = parseInt(timeMatch[1], 10);
      const minutes = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
      targetDate.setHours(hours, minutes, 0, 0);

      if (targetDate.getTime() <= now.getTime() && !content.includes('hoje')) {
        targetDate = new Date(targetDate.getTime() + 24 * 60 * 60 * 1000);
      }

      return { triggerAt: targetDate.getTime(), recurrence: null };
    }

    const hourMatch = content.match(/(\d{1,2})\s*h(?:oras)?\b/i);
    if (hourMatch) {
      const hours = parseInt(hourMatch[1], 10);
      targetDate.setHours(hours, 0, 0, 0);

      if (targetDate.getTime() <= now.getTime() && !content.includes('hoje')) {
        targetDate = new Date(targetDate.getTime() + 24 * 60 * 60 * 1000);
      }

      return { triggerAt: targetDate.getTime(), recurrence: null };
    }

    if (content.includes('hoje') || content.includes('amanhã') || content.includes('amanha') || content.includes('depois de amanhã') || content.includes('depois de amanha')) {
      targetDate.setHours(targetDate.getHours(), 0, 0, 0);
      if (targetDate.getTime() <= now.getTime()) {
        targetDate = new Date(targetDate.getTime() + 60 * 60 * 1000);
      }
      return { triggerAt: targetDate.getTime(), recurrence: null };
    }

    return null;
  }

  private parseRecurring(content: string, now: Date): { triggerAt: number; recurrence: string | null } | null {
    if (content.includes('todo dia') || content.includes('todos os dias')) {
      const timeMatch = content.match(/(\d{1,2})[:h](\d{2})?\s*(h|horas)?/i);
      const hours = timeMatch ? parseInt(timeMatch[1], 10) : 9;
      const minutes = timeMatch && timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;

      const cronExpr = `${minutes} ${hours} * * *`;
      const next = new Date(now);
      next.setHours(hours, minutes, 0, 0);
      if (next.getTime() <= now.getTime()) {
        next.setDate(next.getDate() + 1);
      }
      return { triggerAt: next.getTime(), recurrence: cronExpr };
    }

    if (content.includes('toda semana') || content.includes('todas as semanas') || content.includes('semanalmente')) {
      const cronExpr = `0 9 * * 1`;
      const next = new Date(now);
      const daysUntilMonday = (1 - now.getDay() + 7) % 7 || 7;
      next.setDate(now.getDate() + daysUntilMonday);
      next.setHours(9, 0, 0, 0);
      return { triggerAt: next.getTime(), recurrence: cronExpr };
    }

    if (content.includes('todo mês') || content.includes('todos os meses') || content.includes('mensalmente')) {
      const dayMatch = content.match(/dia\s+(\d{1,2})/i);
      const day = dayMatch ? parseInt(dayMatch[1], 10) : 1;
      const cronExpr = `0 9 ${day} * *`;
      const next = new Date(now);
      next.setDate(day);
      next.setHours(9, 0, 0, 0);
      if (next.getTime() <= now.getTime()) {
        next.setMonth(next.getMonth() + 1);
      }
      return { triggerAt: next.getTime(), recurrence: cronExpr };
    }

    for (const [dayName, dayNum] of Object.entries(WEEKDAYS)) {
      if ((content.includes('toda') || content.includes('todo')) && content.includes(dayName)) {
        const timeMatch = content.match(/(\d{1,2})[:h](\d{2})?\s*(h|horas)?/i);
        const hours = timeMatch ? parseInt(timeMatch[1], 10) : 9;
        const minutes = timeMatch && timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;

        const cronExpr = `${minutes} ${hours} * * ${dayNum}`;
        const next = new Date(now);
        const diff = (dayNum - now.getDay() + 7) % 7 || 7;
        next.setDate(now.getDate() + diff);
        next.setHours(hours, minutes, 0, 0);
        return { triggerAt: next.getTime(), recurrence: cronExpr };
      }
    }

    return null;
  }

  private hasTimeReference(content: string): boolean {
    const timeWords = [
      'agora', 'já', 'hoje', 'amanhã', 'amanha', 'depois de amanhã', 'depois de amanha',
      'próxima semana', 'proxima semana', 'em ', 'daqui ',
      'segundo', 'segundos', 'hora', 'horas', 'h', 'minuto', 'minutos',
      'dia', 'dias', 'semana', 'semanas',
      'todo dia', 'toda semana', 'todo mês',
      ...Object.keys(WEEKDAYS),
    ];
    return timeWords.some((w) => content.includes(w)) || /\d{1,2}[:h]\d{2}/i.test(content) || /\d+\s*(segundo|minuto|hora|dia|semana)/i.test(content);
  }

  private extractReminderMessage(rawContent: string, parsed: { triggerAt: number; recurrence: string | null }): string {
    let msg = rawContent;

    const lines = msg.split(/\n+/).map((l) => l.trim()).filter(Boolean);
    if (lines.length > 1) {
      const unique = [...new Set(lines)];
      msg = unique.sort((a, b) => b.length - a.length)[0];
    }

    msg = msg.replace(/lembr\w*\s*(me|de|que|do|da)?\s*(de|que|do|da)?\s*/gi, '');
    msg = msg.replace(/lenbr\w*\s*(me|de|que|do|da)?\s*(de|que|do|da)?\s*/gi, '');
    msg = msg.replace(/me\s*lembr\w*\s*(de|que|do|da)?\s*/gi, '');
    msg = msg.replace(/me\s*lenbr\w*\s*(de|que|do|da)?\s*/gi, '');
    msg = msg.replace(/avi[zs]\w*\s*(me)?\s*(que|de|sobre)?\s*/gi, '');
    msg = msg.replace(/me\s*avi[zs]\w*\s*(que|de|sobre)?\s*/gi, '');
    msg = msg.replace(/^me\s+(de\s+)?/i, '');
    msg = msg.replace(/\s+me\s+/i, ' ');
    msg = msg.replace(/agendar\s*/gi, '');
    msg = msg.replace(/agenda\s*/gi, '');
    msg = msg.replace(/alarme\s*(para)?\s*/gi, '');

    msg = msg.replace(/em\s+\d+\s*(segundo|minuto|hora|dia|semana)s?\s*/gi, '');
    msg = msg.replace(/daqui\s+(a\s+)?\d+\s*(segundo|minuto|hora|dia|semana)s?\s*/gi, '');
    msg = msg.replace(/amanhã|amanha/gi, '');
    msg = msg.replace(/hoje/gi, '');
    msg = msg.replace(/depois de amanhã|depois de amanha/gi, '');
    msg = msg.replace(/próxim[ao]s?\s*(segunda|terça|terca|quarta|quinta|sexta|sábado|sabado|domingo|semana)/gi, '');
    msg = msg.replace(/proxim[ao]s?\s*(segunda|terça|terca|quarta|quinta|sexta|sábado|sabado|domingo|semana)/gi, '');
    msg = msg.replace(/\b(segunda|terça|terca|quarta|quinta|sexta|sábado|sabado|domingo)(\s*-?\s*feira)?\b/gi, '');
    msg = msg.replace(/(?:\s|^)[àa]s?\s*\d{1,2}[:h]\d{2}?\s*(h|horas)?/gi, '');
    msg = msg.replace(/\d{1,2}[:h]\d{2}?\s*(h|horas)?/gi, '');
    msg = msg.replace(/(?:\s|^)[àa]s?\s*\d{1,2}\s*h(?:oras)?/gi, '');
    msg = msg.replace(/\d{1,2}\s*h(?:oras)?\b/gi, '');
    msg = msg.replace(/todo\s*(dia|mês|mes|semana)/gi, '');
    msg = msg.replace(/toda\s*(semana)/gi, '');
    msg = msg.replace(/todos?\s*os?\s*(dias|mês|mes|semanas)/gi, '');
    msg = msg.replace(/todas\s*as\s*semanas/gi, '');
    msg = msg.replace(/mensalmente|semanalmente/gi, '');
    msg = msg.replace(/\d{1,2}[\/\-]\d{1,2}([\/\-]\d{2,4})?/g, '');

    msg = msg.replace(/\s+/g, ' ').trim();

    if (!msg || msg.length < 2) {
      msg = 'Lembrete';
    }

    if (msg.length > 200) {
      msg = msg.substring(0, 200) + '...';
    }

    return msg;
  }

  private parseSnoozeMinutes(content: string): number {
    const match = content.match(/(\d+)\s*(minuto|minutos|min|m)/i);
    if (match) return parseInt(match[1], 10);

    const hourMatch = content.match(/(\d+)\s*(hora|horas|h)/i);
    if (hourMatch) return parseInt(hourMatch[1], 10) * 60;

    if (content.includes('1h') || content.includes('1 h')) return 60;
    if (content.includes('2h') || content.includes('2 h')) return 120;

    return SCHEDULER_CONFIG.snoozeMinutes;
  }

  private scheduleReminder(reminder: Reminder): void {
    if (reminder.recurrence) {
      this.scheduleRecurring(reminder);
    } else {
      this.scheduleOneTime(reminder);
    }
  }

  private scheduleOneTime(reminder: Reminder): void {
    const delay = reminder.triggerAt - Date.now();
    if (delay <= 0) {
      this.fireReminder(reminder.id);
      return;
    }

    const timer = setTimeout(() => {
      this.fireReminder(reminder.id);
    }, delay);

    (timer as any).unref?.();

    this.cronJobs.set(reminder.id, timer as any);
  }

  private scheduleRecurring(reminder: Reminder): void {
    if (!cron.validate(reminder.recurrence!)) {
      this.logger.warn(`[SchedulerAgent] Invalid cron expression: ${reminder.recurrence}`);
      this.scheduleOneTime(reminder);
      return;
    }

    const task = cron.schedule(reminder.recurrence!, () => {
      this.fireReminder(reminder.id);
    }, {
      timezone: SCHEDULER_CONFIG.timezone,
    });

    this.cronJobs.set(reminder.id, task);
  }

  private fireReminder(id: string): void {
    const reminder = this.reminders.get(id);
    if (!reminder || reminder.status === 'cancelled' || reminder.status === 'done') return;

    reminder.status = 'fired';
    reminder.firedAt = Date.now();
    this.updateReminderInDb(reminder);

    this.logger.log(`[SchedulerAgent] 🔔 Firing reminder: "${reminder.message}" for ${reminder.remoteJid}`);

    if (this.onFireCallback) {
      this.onFireCallback(reminder);
    }

    if (!reminder.recurrence) {
      this.cancelCronJob(id);
    }
  }

  async rescheduleReminder(id: string, delayMs: number): Promise<void> {
    const reminder = this.reminders.get(id);
    if (!reminder) return;

    reminder.status = 'pending';
    reminder.firedAt = null;
    reminder.triggerAt = Date.now() + delayMs;

    this.cancelCronJob(id);
    this.scheduleReminder(reminder);
    this.updateReminderInDb(reminder);

    this.logger.log(`[SchedulerAgent] Rescheduled reminder "${reminder.message}" in ${Math.round(delayMs / 1000)}s`);
  }

  private cancelReminder(id: string): void {
    const reminder = this.reminders.get(id);
    if (!reminder) return;

    reminder.status = 'cancelled';
    this.cancelCronJob(id);
    this.updateReminderInDb(reminder);
  }

  private cancelCronJob(id: string): void {
    const job = this.cronJobs.get(id);
    if (job) {
      if (typeof (job as any).stop === 'function') {
        (job as any).stop();
      } else {
        clearTimeout(job as any);
      }
      this.cronJobs.delete(id);
    }
  }

  private getUserReminders(remoteJid: string, botId: string): Reminder[] {
    return Array.from(this.reminders.values()).filter(
      (r) => r.remoteJid === remoteJid && r.botId === botId,
    );
  }

  markDone(remoteJid: string, botId: string): boolean {
    const reminders = this.getUserReminders(remoteJid, botId).filter(
      (r) => r.status === 'fired' || r.status === 'snoozed' || r.status === 'pending',
    );
    if (reminders.length === 0) return false;

    const reminder = reminders[0];
    reminder.status = 'done';
    this.cancelCronJob(reminder.id);
    this.updateReminderInDb(reminder);
    this.logger.log(`[SchedulerAgent] Marked done: "${reminder.message}"`);
    return true;
  }

  isDoneVerb(content: string): boolean {
    const lower = content.toLowerCase().trim();
    return DONE_VERBS.some((v) => lower.includes(v));
  }

  public listRemindersForUser(remoteJid: string, botId?: string): Reminder[] {
    const all = Array.from(this.reminders.values()).filter((r) => r.remoteJid === remoteJid);
    if (botId) return all.filter((r) => r.botId === botId);
    return all;
  }

  public listAllReminders(): Reminder[] {
    return Array.from(this.reminders.values());
  }

  public cancelReminderById(id: string): boolean {
    const reminder = this.reminders.get(id);
    if (!reminder) return false;
    this.cancelReminder(id);
    return true;
  }

  public createReminderProgrammatic(params: {
    remoteJid: string;
    botId: string;
    message: string;
    triggerAt: number;
    recurrence?: string | null;
  }): Reminder {
    const { remoteJid, botId, message, triggerAt, recurrence } = params;

    const reminder: Reminder = {
      id: this.generateId(),
      remoteJid,
      botId,
      message,
      triggerAt,
      recurrence: recurrence || null,
      status: 'pending',
      snoozeCount: 0,
      createdAt: Date.now(),
      firedAt: null,
    };

    this.reminders.set(reminder.id, reminder);
    this.scheduleReminder(reminder);
    this.saveReminderToDb(reminder);

    this.logger.log(`[SchedulerAgent] Programmatic reminder created: "${message}" for ${remoteJid} at ${this.formatDate(triggerAt)}`);

    return reminder;
  }

  public getReminderStats(): { total: number; pending: number; fired: number; done: number; cancelled: number; snoozed: number } {
    const all = Array.from(this.reminders.values());
    return {
      total: all.length,
      pending: all.filter((r) => r.status === 'pending').length,
      fired: all.filter((r) => r.status === 'fired').length,
      done: all.filter((r) => r.status === 'done').length,
      cancelled: all.filter((r) => r.status === 'cancelled').length,
      snoozed: all.filter((r) => r.status === 'snoozed').length,
    };
  }

  private startPoll(): void {
    if (this.pollTimer) return;

    this.pollTimer = setInterval(() => {
      const now = Date.now();
      for (const reminder of this.reminders.values()) {
        if (reminder.status === 'pending' && reminder.triggerAt <= now) {
          this.fireReminder(reminder.id);
        }
      }
    }, SCHEDULER_CONFIG.pollIntervalMs);

    (this.pollTimer as any).unref?.();
  }

  stopPoll(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  destroy(): void {
    this.stopPoll();
    for (const id of this.cronJobs.keys()) {
      this.cancelCronJob(id);
    }
    this.reminders.clear();
  }

  private async loadFromDb(): Promise<void> {
    if (!this.prisma?.schedulerReminder) return;

    const rows = await this.prisma.schedulerReminder.findMany({
      where: {
        status: { in: ['pending', 'snoozed'] },
      },
    });

    const now = Date.now();
    let restored = 0;
    for (const row of rows) {
      const reminder: Reminder = {
        id: row.id,
        remoteJid: row.remoteJid,
        botId: row.botId,
        message: row.message,
        triggerAt: new Date(row.triggerAt).getTime(),
        recurrence: row.recurrence,
        status: row.status as Reminder['status'],
        snoozeCount: row.snoozeCount,
        createdAt: new Date(row.createdAt).getTime(),
        firedAt: row.firedAt ? new Date(row.firedAt).getTime() : null,
      };

      this.reminders.set(reminder.id, reminder);

      if (reminder.triggerAt > now) {
        this.scheduleReminder(reminder);
        restored++;
      } else {
        this.fireReminder(reminder.id);
      }
    }

    if (restored > 0) {
      this.logger.log(`[SchedulerAgent] Loaded ${restored} reminders from DB`);
    }
  }

  private async saveReminderToDb(reminder: Reminder): Promise<void> {
    if (!this.prisma?.schedulerReminder) return;
    try {
      await this.prisma.schedulerReminder.create({
        data: {
          id: reminder.id,
          remoteJid: reminder.remoteJid,
          botId: reminder.botId,
          message: reminder.message,
          triggerAt: new Date(reminder.triggerAt),
          recurrence: reminder.recurrence,
          status: reminder.status,
          snoozeCount: reminder.snoozeCount,
          firedAt: reminder.firedAt ? new Date(reminder.firedAt) : null,
        },
      });
    } catch (err: any) {
      this.logger.warn(`[SchedulerAgent] Failed to save reminder to DB: ${err.message}`);
    }
  }

  private async updateReminderInDb(reminder: Reminder): Promise<void> {
    if (!this.prisma?.schedulerReminder) return;
    try {
      await this.prisma.schedulerReminder.update({
        where: { id: reminder.id },
        data: {
          status: reminder.status,
          snoozeCount: reminder.snoozeCount,
          triggerAt: new Date(reminder.triggerAt),
          firedAt: reminder.firedAt ? new Date(reminder.firedAt) : null,
        },
      });
    } catch (err: any) {
      this.logger.warn(`[SchedulerAgent] Failed to update reminder in DB: ${err.message}`);
    }
  }

  restoreReminders(reminders: Reminder[]): void {
    const now = Date.now();
    for (const r of reminders) {
      if (r.status === 'pending' || r.status === 'snoozed') {
        this.reminders.set(r.id, r);
        if (r.triggerAt > now) {
          this.scheduleReminder(r);
        } else {
          this.fireReminder(r.id);
        }
      }
    }
    this.logger.log(`[SchedulerAgent] Restored ${reminders.length} reminders from persistence`);
  }

  getPendingReminders(): Reminder[] {
    return Array.from(this.reminders.values()).filter(
      (r) => r.status === 'pending' || r.status === 'snoozed',
    );
  }

  private generateId(): string {
    return `rem_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  }

  private formatDate(timestamp: number): string {
    const date = new Date(timestamp);
    const today = new Date();
    const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);

    const isToday = date.toDateString() === today.toDateString();
    const isTomorrow = date.toDateString() === tomorrow.toDateString();

    const time = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    const dateStr = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });

    if (isToday) return `hoje às ${time}`;
    if (isTomorrow) return `amanhã às ${time}`;
    return `${dateStr} às ${time}`;
  }
}
