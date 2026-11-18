/**
 * Seed script to persist TRD Knowledge Vault into PostgreSQL (RagDocument & RagChunk)
 */

import { PrismaClient } from '@prisma/client';
import * as fs from 'fs';
import * as path from 'path';

async function seedDatabase() {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║       TRD EVOLUTION — População de Banco PostgreSQL      ║');
  console.log('╚══════════════════════════════════════════════════════════╝\n');

  const prisma = new PrismaClient();
  const botId = 'trd-bot-main';
  const vaultPath = path.resolve(__dirname, '../src/api/integrations/chatbot/anthropic/agents/trd-knowledge-vault.json');

  if (!fs.existsSync(vaultPath)) {
    throw new Error('Vault file not found: ' + vaultPath);
  }

  const vault = JSON.parse(fs.readFileSync(vaultPath, 'utf8'));
  const chunks = vault.chunks || [];

  console.log(`  Carregados ${chunks.length} chunks do vault.`);

  // Group chunks by documentTitle
  const docsMap = new Map<string, any[]>();
  for (const c of chunks) {
    const list = docsMap.get(c.documentTitle) || [];
    list.push(c);
    docsMap.set(c.documentTitle, list);
  }

  // Clear existing TRD documents for this bot
  await prisma.ragDocument.deleteMany({
    where: { botId },
  });
  console.log('  Base anterior limpa para o bot:', botId);

  let totalDocs = 0;
  let totalChunks = 0;

  for (const [title, docChunks] of docsMap.entries()) {
    const doc = await prisma.ragDocument.create({
      data: {
        botId,
        title,
        source: docChunks[0]?.sourceFile || title,
        content: docChunks.map((c: any) => c.content).join('\n\n'),
      },
    });

    for (let i = 0; i < docChunks.length; i++) {
      await prisma.ragChunk.create({
        data: {
          documentId: doc.id,
          content: `[${docChunks[i].section}] ${docChunks[i].content}`,
          embedding: null,
          index: i,
        },
      });
      totalChunks++;
    }

    totalDocs++;
    console.log(`  ✅ Inserido documento [${title}] com ${docChunks.length} chunks.`);
  }

  console.log(`\n  🎉 PostgreSQL sincronizado! Total: ${totalDocs} documentos, ${totalChunks} chunks.`);
  await prisma.$disconnect();
}

seedDatabase().catch((err) => {
  console.error('Erro ao popular banco:', err);
  process.exit(1);
});
