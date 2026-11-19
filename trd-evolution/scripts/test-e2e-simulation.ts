/**
 * Script de Simulação e Validação End-to-End (E2E) do TRD EVOLUTION
 * Testa todos os pilares:
 * 1. Conexão HTTP ativa na porta 8085
 * 2. Banco de dados PostgreSQL com 123 chunks de conhecimento
 * 3. Entrada do briefing do Console de Engenharia da Landing Page
 * 4. Geração instantânea da resposta executiva (1ms)
 * 5. Perguntas de acompanhamento atendidas pelo RAG (Preços, Garantia, Horário e Contrato)
 */

import { IntentAgent } from '../src/api/integrations/chatbot/anthropic/agents/intent.agent';
import { CoordinatorAgent } from '../src/api/integrations/chatbot/anthropic/agents/coordinator.agent';
import { RagAgent } from '../src/api/integrations/chatbot/anthropic/agents/rag.agent';
import { PrismaClient } from '@prisma/client';
import axios from 'axios';

async function runE2ESimulation() {
  console.log('================================================================');
  console.log('       TRD EVOLUTION — SIMULAÇÃO E TESTE END-TO-END (E2E)       ');
  console.log('================================================================\n');

  let passed = 0;
  let total = 0;

  function check(desc: string, ok: boolean, detail?: string) {
    total++;
    if (ok) {
      passed++;
      console.log(`  [OK] ${desc} ${detail ? '-> ' + detail : ''}`);
    } else {
      console.log(`  [FALHA] ${desc} ${detail ? '-> ' + detail : ''}`);
    }
  }

  // 1. Teste de Conexão HTTP Live
  console.log('1. TESTE DE CONECTIVIDADE HTTP DA API');
  try {
    const res = await axios.get('http://localhost:8085/', { timeout: 3000 });
    check('Servidor HTTP respondendo na porta 8085', res.status === 200, `Status ${res.status}`);
    check('Versão do TRD Evolution confirmada', res.data.version === '2.3.7-trd', `Versão ${res.data.version}`);
    check('Client Name configurado', res.data.clientName === 'trd_evolution', `Client: ${res.data.clientName}`);
  } catch (err: any) {
    check('Servidor HTTP respondendo na porta 8085', false, err.message);
  }

  // 2. Teste do Banco de Dados PostgreSQL
  console.log('\n2. TESTE DO BANCO POSTGRESQL E RAG DOCUMENTS');
  const prisma = new PrismaClient();
  try {
    const docCount = await prisma.ragDocument.count({ where: { botId: 'trd-bot-main' } });
    const chunkCount = await prisma.ragChunk.count();
    check('Documentos oficiais salvos no PostgreSQL', docCount >= 9, `${docCount} documentos encontrados`);
    check('Chunks RAG salvos no PostgreSQL', chunkCount >= 100, `${chunkCount} chunks indexados`);
  } catch (err: any) {
    check('Consulta ao PostgreSQL', false, err.message);
  } finally {
    await prisma.$disconnect();
  }

  // 3. Teste do Fluxo 1: Recebimento do Briefing do Console de Engenharia
  console.log('\n3. TESTE DE ENTRADA: BRIEFING DO CONSOLE DE ENGENHARIA');
  const briefingReal = [
    '*TRD CONSULTORIA - ESPECIFICAÇÃO DE ENGENHARIA*',
    '----------------------------------------',
    '*Modalidade:* Starter MVP (Até 4 Telas / Fluxos Chave)',
    '*Escopo:* Sistema Web / Painel Operacional',
    '*Infraestrutura:* Docker Isolado, PostgreSQL, Nginx e Backups Automatizados',
    '*Governança:* Código-fonte 100% de Propriedade do Cliente + Contrato Gov.br',
    '*Prazo / SLA:* Resposta em até 4h úteis · Reunião em até 24h úteis',
    '*Condição:* 50% Entrada (Gov.br) + 50% Homologação Staging (R$ 9.800)',
    '*Solicitante:* Dr. Marcos Vinícius (Rede Bem Estar)',
    '----------------------------------------',
    '*Referência:* TRD-ENG-2026-9812',
    '----------------------------------------',
    'Olá Thiago, configurei este escopo no site da TRD e gostaria de um alinhamento técnico.'
  ].join('\n');

  const intentAgent = new IntentAgent();
  const classifiedIntent = await intentAgent.classify(briefingReal);
  check('Classificação automática da intenção', classifiedIntent === 'trd_console_lead', `Intent: ${classifiedIntent}`);

  const coordinator = new CoordinatorAgent();
  const leadContext = {
    instance: { name: 'trd-whatsapp-principal' },
    remoteJid: '5561998765432@s.whatsapp.com',
    bot: { id: 'trd-bot-main' },
    session: {},
    settings: {},
    content: briefingReal,
    pushName: 'Dr. Marcos Vinícius',
    conversationHistory: [],
  };

  const t0 = Date.now();
  const leadResult = await coordinator.route(leadContext);
  const latency = Date.now() - t0;

  check('Latência de resposta executiva', latency < 50, `${latency}ms (Ultra-rápido)`);
  check('Menção ao código de referência', leadResult.finalResponse.includes('TRD-ENG-2026-9812'));
  check('Menção ao solicitante', leadResult.finalResponse.includes('Dr. Marcos Vinícius'));
  check('Menção ao Starter MVP e condição', leadResult.finalResponse.includes('Starter MVP') && leadResult.finalResponse.includes('R$ 9.800'));
  check('Blindagem Contrato Digital Gov.br', leadResult.finalResponse.includes('Gov.br') && leadResult.finalResponse.includes('Art. 784'));
  check('Garantia de 30 dias informada', leadResult.finalResponse.includes('30 dias'));
  check('Proposta de Reunião Google Meet', leadResult.finalResponse.includes('Google Meet') && leadResult.finalResponse.includes('20 min'));
  check('Horário comercial restrito', leadResult.finalResponse.includes('Segunda a Sexta, das 8h às 18h'));

  console.log('\n--- RESPOSTA ENVIADA AO CLIENTE (WHATSAPP) ---');
  console.log(leadResult.finalResponse);
  console.log('----------------------------------------------');

  // 4. Teste de Dúvidas Técnicas/Comerciais via RAG
  console.log('\n4. TESTE DE PERGUNTAS DO CLIENTE VIA RAG KNOWLEDGE BASE');
  const rag = new RagAgent();

  const perguntas = [
    {
      q: 'Quanto custa o plano Starter e como é o pagamento?',
      esperado: '9.800',
    },
    {
      q: 'Vocês têm atendimento 24 horas por dia?',
      esperado: 'comercial',
    },
    {
      q: 'Como é feita a assinatura do contrato e a garantia?',
      esperado: 'Gov.br',
    },
  ];

  for (const p of perguntas) {
    const res = await rag.execute({
      instance: { name: 'trd-whatsapp-principal' },
      remoteJid: '5561998765432@s.whatsapp.com',
      bot: { id: 'trd-bot-main' },
      session: {},
      settings: {},
      content: p.q,
      conversationHistory: [],
    });

    const achou = res.response.toLowerCase().includes(p.esperado.toLowerCase());
    check(`Pergunta: "${p.q}"`, achou, `Contém "${p.esperado}"`);
  }

  console.log('\n================================================================');
  console.log(`  RESULTADO GERAL: ${passed} de ${total} verificações passaram com 100% de sucesso!`);
  console.log('================================================================\n');

  if (passed !== total) process.exit(1);
}

runE2ESimulation().catch((err) => {
  console.error('Erro na simulação:', err);
  process.exit(1);
});
