/**
 * Test script for TRD EVOLUTION RAG Knowledge Base Retrieval
 */

import { RagAgent } from '../src/api/integrations/chatbot/anthropic/agents/rag.agent';
import { AgentContext } from '../src/api/integrations/chatbot/anthropic/agents/agent-types';

function buildContext(query: string): AgentContext {
  return {
    instance: { name: 'trd-instance' },
    remoteJid: '5561999999999@s.whatsapp.com',
    bot: { id: 'trd-bot-main' },
    session: {},
    settings: {},
    content: query,
    conversationHistory: [],
  };
}

async function runRagTests() {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║       TRD EVOLUTION — Teste de RAG Knowledge Vault       ║');
  console.log('╚══════════════════════════════════════════════════════════╝\n');

  const rag = new RagAgent();
  let passed = 0;
  let failed = 0;

  function assert(cond: boolean, msg: string) {
    if (cond) {
      console.log(`  ✅ ${msg}`);
      passed++;
    } else {
      console.log(`  ❌ ${msg}`);
      failed++;
    }
  }

  // Test 1: Starter Pricing & Scope
  console.log('━━━ 1. Consulta: Preço e Condição do Starter ━━━');
  const res1 = await rag.execute(buildContext('qual o valor e as condições de pagamento do plano starter mvp?'));
  assert(res1.status === 'completed', 'RAG query 1 completed successfully');
  assert(res1.response.length > 50, 'RAG returned rich context');
  assert(res1.response.includes('9.800') || res1.response.includes('STARTER'), 'Context includes Starter / R$ 9.800');
  console.log(`  Context snippet: ${res1.response.substring(0, 160).replace(/\n/g, ' ')}...\n`);

  // Test 2: Warranty & Governance
  console.log('━━━ 2. Consulta: Garantia e Sustentação ━━━');
  const res2 = await rag.execute(buildContext('como funciona a garantia e o suporte de sustentação pós-entrega?'));
  assert(res2.status === 'completed', 'RAG query 2 completed successfully');
  assert(res2.response.includes('30 dias') || res2.response.includes('garantia') || res2.response.includes('sustentação'), 'Context includes 30 days warranty / support');
  console.log(`  Context snippet: ${res2.response.substring(0, 160).replace(/\n/g, ' ')}...\n`);

  // Test 3: Tech Stack
  console.log('━━━ 3. Consulta: Stack de Engenharia ━━━');
  const res3 = await rag.execute(buildContext('qual a stack e arquitetura de tecnologia utilizada pela consultoria?'));
  assert(res3.status === 'completed', 'RAG query 3 completed successfully');
  assert(res3.response.includes('Docker') || res3.response.includes('PostgreSQL') || res3.response.includes('Angular') || res3.response.includes('FastAPI'), 'Context includes official tech stack');
  console.log(`  Context snippet: ${res3.response.substring(0, 160).replace(/\n/g, ' ')}...\n`);

  // Test 4: Digital Contract & Legal Shield
  console.log('━━━ 4. Consulta: Contrato Digital Gov.br ━━━');
  const res4 = await rag.execute(buildContext('como é formalizado o contrato e a assinatura digital?'));
  assert(res4.status === 'completed', 'RAG query 4 completed successfully');
  assert(res4.response.includes('Gov.br') || res4.response.includes('CONTRATO') || res4.response.includes('784'), 'Context includes Gov.br contract / legal validity');
  console.log(`  Context snippet: ${res4.response.substring(0, 160).replace(/\n/g, ' ')}...\n`);

  console.log(`Results: ${passed} passed, ${failed} failed.`);
  if (failed > 0) process.exit(1);
}

runRagTests().catch((err) => {
  console.error(err);
  process.exit(1);
});
