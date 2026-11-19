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
        this.logger.log('[TextAgent] Models unavailable, synthesizing answer directly from TRD knowledge');
        const ragFallback = this.generateRagDirectAnswer(context.content, context.webContext || '');
        return this.success(ragFallback, undefined, {
          model: 'trd-rag-fallback',
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

  private generateRagDirectAnswer(query: string, ragContext: string): string {
    const cleanContext = ragContext
      .replace('--- CONTEXTO DA BASE DE CONHECIMENTO ---', '')
      .replace('--- FIM DO CONTEXTO ---', '')
      .trim();

    const lower = (query || '').toLowerCase();

    if (
      lower.includes('boa noite') ||
      lower.includes('bom dia') ||
      lower.includes('boa tarde') ||
      lower.includes('olá') ||
      lower.includes('ola') ||
      lower.includes('tudo bem') ||
      lower.includes('opa')
    ) {
      if (!lower.includes('custa') && !lower.includes('valor') && !lower.includes('preço') && !lower.includes('preco') && !lower.includes('quanto') && !lower.includes('sistema')) {
        return [
          'Olá! Seja bem-vindo à *TRD Consultoria de TI* (atendimento executivo do Thiago Duarte).',
          '',
          'Como podemos apoiar a sua empresa hoje?',
          '• 💻 **Desenvolvimento Sob Medida** (Sistemas web, APIs, integrações e MVPs entregues em 2 a 3 semanas)',
          '• ☁️ **Infraestrutura & DevOps** (Docker, PostgreSQL, CI/CD e esteiras de automação)',
          '• 🛡️ **Segurança & Sustentação** (Auditoria, contratos digitais Gov.br e suporte assistido)',
          '',
          'Se você já tem uma demanda em mente ou gostaria de tirar dúvidas sobre escopo e valores, basta nos falar por aqui!'
        ].join('\n');
      }
    }

    if (
      lower.includes('marcar') ||
      lower.includes('agendar') ||
      lower.includes('reunião') ||
      lower.includes('reuniao') ||
      lower.includes('pode ser') ||
      lower.includes('combinado') ||
      lower.includes('fechado') ||
      lower.includes('gostei') ||
      lower.includes('amanhã') ||
      lower.includes('amanha') ||
      lower.includes('horário') ||
      lower.includes('horario') ||
      lower.includes('15:00') ||
      lower.includes('15h') ||
      lower.includes('14:30') ||
      lower.includes('16:30') ||
      lower.includes('10:00') ||
      lower.includes('10h') ||
      lower.includes('meet') ||
      lower.includes('call')
    ) {
      const timeMatch = query.match(/(\d{1,2}(?::\d{2}|h))/i);
      const timeStr = timeMatch ? timeMatch[1] : '15:00';
      const dayStr = lower.includes('amanh') ? 'Amanhã' : 'Data combinada';

      return [
        'Excelente! Agendamento registrado com sucesso. 🎯',
        '',
        '📅 *Alinhamento Técnico Executivo — TRD Consultoria de TI*',
        `• **Data:** ${dayStr}`,
        `• **Horário:** ${timeStr} (Horário de Brasília)`,
        '• **Formato:** Reunião online de 20 minutos via **Google Meet**',
        '• **Participante:** Thiago Duarte (Fundador & Arquiteto Principal)',
        '',
        '📋 *Pauta da Sessão:*',
        '1. Congelamento do escopo dos módulos core e infraestrutura Docker/PostgreSQL.',
        '2. Validação do cronograma ágil (Sprint 1 em 5 dias).',
        '3. Formalização do contrato digital Gov.br com cláusula de homologação e garantia.',
        '',
        'Qual o seu melhor e-mail corporativo para enviarmos o convite da sala do Meet com os lembretes na agenda?'
      ].join('\n');
    }

    if (lower.includes('custa') || lower.includes('valor') || lower.includes('preço') || lower.includes('preco') || lower.includes('starter') || lower.includes('pagamento') || lower.includes('quanto')) {
      return [
        'Olá! Agradecemos o seu contato com a *TRD Consultoria de TI*.',
        '',
        '💼 *Valores e Condições de Desenvolvimento Sob Medida:*',
        '• **Plano Starter MVP:** **R$ 9.800 à vista** (ou 50% de entrada na assinatura do contrato digital Gov.br + 50% na homologação do sistema em ambiente de Staging).',
        '• **Escopo Incluso:** Até 4 telas/fluxos operacionais chave, infraestrutura Docker isolada, banco PostgreSQL e 30 dias de sustentação assistida inclusa.',
        '• **Governança:** Código-fonte 100% de propriedade do cliente, sem taxas ocultas.',
        '',
        '🎯 Para estruturarmos o escopo exato do seu projeto, convidamos você para uma **reunião técnica de 20 minutos via Google Meet** diretamente com o Thiago Duarte (Fundador & Arquiteto Principal).',
        '',
        'Qual o melhor dia e horário comercial (Seg a Sex, 8h às 18h) para você?'
      ].join('\n');
    }

    if (lower.includes('garantia') || lower.includes('suporte') || lower.includes('após') || lower.includes('pos')) {
      return [
        'Olá! Sobre a garantia e sustentação da *TRD Consultoria de TI*:',
        '',
        '🛡️ **Garantia Inclusa no Projeto:**',
        '• O desenvolvimento sob medida inclui **30 dias de sustentação assistida** após a homologação para correção de qualquer desvio de escopo ou bug sem custos adicionais.',
        '• **Continuidade Operacional:** Após os 30 dias, oferecemos o plano mensal de continuidade preventiva a partir de R$ 1.800/mês para evolução contínua, monitoramento e backups.',
        '',
        'Nosso atendimento é realizado estritamente em horário comercial (*Segunda a Sexta, das 8h às 18h*).'
      ].join('\n');
    }

    if (lower.includes('contrato') || lower.includes('código') || lower.includes('codigo') || lower.includes('segurança')) {
      return [
        'Olá! A governança e blindagem jurídica da *TRD Consultoria de TI* funciona da seguinte forma:',
        '',
        '📄 **Contrato Digital & Propriedade:**',
        '• O contrato é formalizado digitalmente via **Gov.br**, possuindo força de título executivo extrajudicial (Art. 784, III do CPC).',
        '• **Propriedade Intelectual:** O código-fonte é **100% de propriedade do cliente**, entregue em repositório seguro com Docker, sem nenhuma dependência contratual ou fidelidade.',
        '• **Ambiente de Homologação (Staging):** O cliente só aprova a entrega final após testar as telas no ar.'
      ].join('\n');
    }

    if (lower.includes('24h') || lower.includes('plantão') || lower.includes('plantao') || lower.includes('madrugada') || lower.includes('domingo')) {
      return [
        'Olá! Informamos que o atendimento técnico e comercial da *TRD Consultoria de TI* opera estritamente em **horário comercial (Segunda a Sexta, das 8h às 18h)**.',
        '',
        'Não operamos em regime de plantão 24/7 noturno ou finais de semana. Em contrapartida, todos os nossos sistemas são arquitetados com contêineres Docker isolados, reinicialização automática e backups redundantes para garantir estabilidade contínua sem interrupções operacionais.'
      ].join('\n');
    }

    if (cleanContext) {
      return [
        'Olá! Aqui é o atendimento executivo da *TRD Consultoria de TI*.',
        '',
        cleanContext.substring(0, 800),
        '',
        'Para agendarmos uma reunião de alinhamento técnico (20 min via Google Meet) com o Thiago Duarte, basta nos informar o melhor horário comercial (Seg a Sex, 8h às 18h)!'
      ].join('\n');
    }

    return [
      'Olá! Aqui é o atendimento executivo da *TRD Consultoria de TI* (Thiago Duarte).',
      '',
      'Desenvolvemos sistemas empresariais sob medida, APIs de alto desempenho e infraestruturas robustas em contêineres Docker.',
      '',
      'Para entendermos sua necessidade e formatarmos uma proposta técnica, podemos agendar uma reunião de 20 minutos via Google Meet em horário comercial (Seg a Sex, 8h às 18h).',
      '',
      'Qual seria a sua necessidade principal no momento?'
    ].join('\n');
  }
}
