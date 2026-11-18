/**
 * Ingestion script for TRD EVOLUTION RAG Knowledge Base.
 * Reads official operational, legal, and commercial guidelines from ../operacoes
 * and compiles them into a structured knowledge vault and database chunks.
 */

import * as fs from 'fs';
import * as path from 'path';

export interface KnowledgeChunk {
  id: string;
  documentTitle: string;
  sourceFile: string;
  section: string;
  content: string;
  keywords: string[];
}

const TARGET_DOCS = [
  '02_comercial_marketing/PLANO_EXPANSAO_NACIONAL_PME.md',
  '02_comercial_marketing/ANEXO_I_FICHA_TECNICA_STARTER_9800.md',
  '02_comercial_marketing/CASES_DE_SUCESSO_E_AUTORIDADE.md',
  '02_comercial_marketing/PROPOSTA_COMERCIAL_EXECUTIVA.md',
  '01_juridico/CONTRATO_DESENVOLVIMENTO_SOFTWARE_SOB_MEDIDA.md',
  '01_juridico/TERMO_HOMOLOGACAO_E_ENTREGA_DEFINITIVA.md',
  '01_juridico/CONTRATO_SUSTENTACAO_E_SLA.md',
  '02_comercial_marketing/PARECER_TECNICO_ARQUITETURA_ENTREGA_SOLO.md',
  '02_comercial_marketing/PROTOCOLO_PROSPECCAO_NACIONAL_ANTIBAN.md',
];

function extractKeywords(text: string): string[] {
  const words = text.toLowerCase()
    .replace(/[^\wà-ú\s]/gi, ' ')
    .split(/\s+/)
    .filter((w) => w.length > 3);
  return Array.from(new Set(words)).slice(0, 20);
}

function chunkDocument(docTitle: string, relPath: string, rawText: string): KnowledgeChunk[] {
  // Split on markdown headers (#, ##, ###) or contract clauses
  const sections = rawText.split(/\n(?=#{1,4}\s+|CLÁUSULA\s+|ANEXO\s+)/i);
  const chunks: KnowledgeChunk[] = [];
  let chunkIdx = 0;

  for (const sec of sections) {
    const trimmed = sec.trim();
    if (!trimmed) continue;

    const firstLine = trimmed.split('\n')[0].replace(/^#{1,4}\s+/, '').trim();
    const sectionTitle = firstLine.length < 120 ? firstLine : docTitle;

    // Split large sections if > 1000 chars
    if (trimmed.length > 1000) {
      const paragraphs = trimmed.split(/\n\n+/);
      let buffer = '';

      for (const p of paragraphs) {
        if ((buffer + '\n\n' + p).length > 900 && buffer.length > 200) {
          chunks.push({
            id: `${relPath}-${chunkIdx++}`,
            documentTitle: docTitle,
            sourceFile: relPath,
            section: sectionTitle,
            content: buffer.trim(),
            keywords: extractKeywords(buffer),
          });
          buffer = p;
        } else {
          buffer = buffer ? `${buffer}\n\n${p}` : p;
        }
      }

      if (buffer.trim()) {
        chunks.push({
          id: `${relPath}-${chunkIdx++}`,
          documentTitle: docTitle,
          sourceFile: relPath,
          section: sectionTitle,
          content: buffer.trim(),
          keywords: extractKeywords(buffer),
        });
      }
    } else {
      chunks.push({
        id: `${relPath}-${chunkIdx++}`,
        documentTitle: docTitle,
        sourceFile: relPath,
        section: sectionTitle,
        content: trimmed,
        keywords: extractKeywords(trimmed),
      });
    }
  }

  return chunks;
}

export async function ingestTRDKnowledge(): Promise<{ totalDocs: number; totalChunks: number; vaultPath: string }> {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║       TRD EVOLUTION — Ingestão de Base RAG Oficial       ║');
  console.log('╚══════════════════════════════════════════════════════════╝\n');

  const baseOperacoesDir = path.resolve(__dirname, '../../operacoes');
  const vaultPath = path.resolve(__dirname, '../src/api/integrations/chatbot/anthropic/agents/trd-knowledge-vault.json');

  if (!fs.existsSync(baseOperacoesDir)) {
    throw new Error(`Diretório de operações não encontrado em: ${baseOperacoesDir}`);
  }

  const allChunks: KnowledgeChunk[] = [];
  let docsProcessed = 0;

  for (const relDoc of TARGET_DOCS) {
    const fullPath = path.join(baseOperacoesDir, relDoc);
    if (!fs.existsSync(fullPath)) {
      console.warn(`  ⚠️ Arquivo não encontrado: ${relDoc}`);
      continue;
    }

    const raw = fs.readFileSync(fullPath, 'utf8');
    const docTitle = path.basename(relDoc, '.md').replace(/_/g, ' ');
    const chunks = chunkDocument(docTitle, relDoc, raw);
    allChunks.push(...chunks);
    docsProcessed++;
    console.log(`  📄 [${docTitle}] processado → ${chunks.length} chunks`);
  }

  // Save compiled knowledge vault
  const vaultData = {
    updatedAt: new Date().toISOString(),
    totalDocs: docsProcessed,
    totalChunks: allChunks.length,
    chunks: allChunks,
  };

  fs.writeFileSync(vaultPath, JSON.stringify(vaultData, null, 2), 'utf8');
  console.log(`\n  ✅ Knowledge Vault salvo com sucesso em:\n     ${vaultPath}`);
  console.log(`  📊 Total de documentos: ${docsProcessed} | Total de Chunks: ${allChunks.length}\n`);

  return { totalDocs: docsProcessed, totalChunks: allChunks.length, vaultPath };
}

// Direct execution
if (require.main === module) {
  ingestTRDKnowledge().catch((err) => {
    console.error('Erro na ingestão:', err);
    process.exit(1);
  });
}
