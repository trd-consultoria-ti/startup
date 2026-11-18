import { AgentContext, AgentResult, RAG_CONFIG } from './agent-types';
import { BaseAgent } from './base.agent';
import OpenAI from 'openai';
import * as fs from 'fs';
import * as path from 'path';

export interface RagChunkResult {
  content: string;
  similarity: number;
  documentTitle: string;
}

export class RagAgent extends BaseAgent {
  private openai: OpenAI | null = null;
  private prisma: any = null;
  private vaultChunks: any[] = [];

  constructor(prismaRepository?: any) {
    super({
      type: 'rag',
      name: 'RAG Agent',
      capabilities: ['knowledge-retrieval', 'semantic-search', 'document-qa', 'context-injection'],
    });
    this.prisma = prismaRepository;
    this.loadVault();

    const apiKey = process.env.OPENAI_API_KEY || process.env.RAG_OPENAI_API_KEY;
    if (apiKey) {
      this.openai = new OpenAI({ apiKey });
    } else {
      this.logger.warn('[RagAgent] No OpenAI API key — embeddings disabled, RAG will use text search fallback');
    }
  }

  private loadVault(): void {
    try {
      const vaultPath = path.resolve(__dirname, 'trd-knowledge-vault.json');
      if (fs.existsSync(vaultPath)) {
        const raw = fs.readFileSync(vaultPath, 'utf8');
        const data = JSON.parse(raw);
        this.vaultChunks = Array.isArray(data.chunks) ? data.chunks : [];
        this.logger.log(`[RagAgent] Loaded ${this.vaultChunks.length} TRD knowledge vault chunks into memory`);
      }
    } catch (err: any) {
      this.logger.warn(`[RagAgent] Could not load TRD knowledge vault: ${err.message}`);
    }
  }

  canHandle(context: AgentContext): boolean {
    const content = this.cleanContent(context.content).toLowerCase().trim();
    if (content.length <= 5) return false;
    return !!this.prisma?.ragChunk || this.vaultChunks.length > 0;
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    try {
      const query = this.cleanContent(context.content);
      const botId = context.bot?.id || '';

      if (!botId && this.vaultChunks.length === 0) {
        return this.success('', undefined, { processingTime: Date.now() - startTime, ragContext: '' });
      }

      const chunks = await this.retrieveRelevant(query, botId);

      if (chunks.length === 0) {
        this.logger.log(`[RagAgent] No relevant chunks found for query: "${query.substring(0, 60)}"`);
        return this.success('', undefined, { processingTime: Date.now() - startTime, ragContext: '', chunksFound: 0 });
      }

      const ragContext = this.formatContext(chunks);
      this.logger.log(
        `[RagAgent] Retrieved ${chunks.length} chunks | topSim=${chunks[0]?.similarity.toFixed(3)} | contextLen=${ragContext.length}`,
      );

      return this.success(ragContext, undefined, {
        processingTime: Date.now() - startTime,
        ragContext,
        chunksFound: chunks.length,
        topSimilarity: chunks[0]?.similarity,
      });
    } catch (error: any) {
      return this.failure(error.message || 'RAG agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  async ingest(botId: string, title: string, content: string, source?: string): Promise<{ chunks: number }> {
    if (!this.prisma?.ragDocument) {
      throw new Error('RAG: Prisma ragDocument model not available');
    }

    const doc = await this.prisma.ragDocument.create({
      data: { botId, title, source: source || null, content },
    });

    const chunks = this.chunkText(content, RAG_CONFIG.chunkSize, RAG_CONFIG.chunkOverlap);
    this.logger.log(`[RagAgent] Ingesting "${title}" → ${chunks.length} chunks`);

    for (let i = 0; i < chunks.length; i++) {
      const embedding = await this.generateEmbedding(chunks[i]);
      await this.prisma.ragChunk.create({
        data: {
          documentId: doc.id,
          content: chunks[i],
          embedding: embedding ? `[${embedding.join(',')}]` : null,
          index: i,
        },
      });
    }

    this.logger.log(`[RagAgent] Ingested "${title}" — ${chunks.length} chunks stored`);
    return { chunks: chunks.length };
  }

  async deleteDocument(documentId: string): Promise<void> {
    if (!this.prisma?.ragDocument) return;
    await this.prisma.ragDocument.delete({ where: { id: documentId } });
    this.logger.log(`[RagAgent] Deleted document ${documentId}`);
  }

  async listDocuments(botId: string): Promise<any[]> {
    if (!this.prisma?.ragDocument) return [];
    return await this.prisma.ragDocument.findMany({
      where: { botId },
      select: { id: true, title: true, source: true, createdAt: true, _count: { select: { chunks: true } } },
      orderBy: { createdAt: 'desc' },
    });
  }

  private async retrieveRelevant(query: string, botId: string): Promise<RagChunkResult[]> {
    if (this.prisma?.ragChunk && botId) {
      const queryEmbedding = await this.generateEmbedding(query);
      if (queryEmbedding) {
        const results = await this.vectorSearch(queryEmbedding, botId);
        if (results.length > 0) return results;
      }

      const textResults = await this.textSearch(query, botId);
      if (textResults.length > 0) return textResults;
    }

    return this.searchVault(query);
  }

  private searchVault(query: string): RagChunkResult[] {
    if (this.vaultChunks.length === 0) return [];

    const stopWords = new Set([
      'o', 'a', 'os', 'as', 'de', 'do', 'da', 'dos', 'das', 'e', 'é', 'ou', 'um', 'uma',
      'que', 'sobre', 'para', 'com', 'em', 'no', 'na', 'nos', 'nas', 'se', 'voce', 'você',
      'vc', 'sao', 'são', 'eh', 'tem', 'têm', 'ser', 'qual', 'quais', 'como', 'quando',
      'onde', 'por', 'sabe', 'sabes', 'gostaria', 'quero', 'quanto'
    ]);
    const queryWords = query
      .toLowerCase()
      .split(/\s+/)
      .map((w) => w.replace(/[^\wà-ú]/gi, '').trim())
      .filter((w) => w.length > 2 && !stopWords.has(w));

    if (queryWords.length === 0) return [];

    const scored = this.vaultChunks
      .map((c) => {
        const lowerText = `${c.documentTitle} ${c.section} ${c.content} ${(c.keywords || []).join(' ')}`.toLowerCase();
        let matches = 0;
        for (const w of queryWords) {
          if (lowerText.includes(w)) matches++;
        }
        const similarity = matches > 0 ? matches / queryWords.length : 0;
        return {
          content: c.content,
          documentTitle: `${c.documentTitle} - ${c.section}`,
          similarity,
        };
      })
      .filter((r) => r.similarity > 0)
      .sort((a, b) => b.similarity - a.similarity)
      .slice(0, RAG_CONFIG.topK);

    return scored;
  }

  private async vectorSearch(queryEmbedding: number[], botId: string): Promise<RagChunkResult[]> {
    try {
      const embeddingStr = `[${queryEmbedding.join(',')}]`;
      const results = await this.prisma.$queryRaw`
        SELECT c.content, d.title as "documentTitle",
               1 - (c.embedding <=> ${embeddingStr}::vector) as similarity
        FROM "RagChunk" c
        JOIN "RagDocument" d ON c."documentId" = d.id
        WHERE d."botId" = ${botId}
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> ${embeddingStr}::vector
        LIMIT ${RAG_CONFIG.topK}
      `;

      return (results as any[])
        .filter((r) => parseFloat(r.similarity) >= RAG_CONFIG.minSimilarity)
        .map((r) => ({
          content: r.content,
          similarity: parseFloat(r.similarity),
          documentTitle: r.documentTitle,
        }));
    } catch (err: any) {
      this.logger.warn(`[RagAgent] Vector search failed: ${err.message} — falling back to text search`);
      return this.textSearch('', botId);
    }
  }

  private async textSearch(query: string, botId: string): Promise<RagChunkResult[]> {
    try {
      const stopWords = new Set(['o', 'a', 'os', 'as', 'de', 'do', 'da', 'dos', 'das', 'e', 'é', 'ou', 'um', 'uma', 'que', 'sobre', 'para', 'com', 'em', 'no', 'na', 'nos', 'nas', 'se', 'voce', 'você', 'vc', 'sao', 'são', 'eh', 'é', 'tem', 'têm', 'ser', 'qual', 'quais', 'como', 'quando', 'onde', 'por', 'sabe', 'sabes', 'the', 'is', 'are', 'of', 'to', 'in', 'on', 'at', 'for', 'and', 'or', 'what', 'which', 'how', 'when', 'where', 'you', 'your', 'about', 'know']);
      const queryWords = query.toLowerCase()
        .split(/\s+/)
        .map(w => w.replace(/[^\wà-ú]/gi, '').trim())
        .filter(w => w.length > 2 && !stopWords.has(w));

      this.logger.log(`[RagAgent] textSearch | query="${query.substring(0, 60)}" | words=[${queryWords.join(',')}] | botId=${botId}`);

      if (queryWords.length === 0) return [];

      const chunks = await this.prisma.ragChunk.findMany({
        where: { document: { botId } },
        include: { document: { select: { title: true } } },
      });

      this.logger.log(`[RagAgent] textSearch | total chunks=${chunks.length} | sample="${chunks[0]?.content?.substring(0, 80) || 'none'}"`);

      const scored = chunks
        .map((c: any) => {
          const lowerContent = c.content.toLowerCase();
          let matches = 0;
          for (const word of queryWords) {
            if (lowerContent.includes(word)) matches++;
          }
          const similarity = matches > 0 ? matches / queryWords.length : 0;
          return {
            content: c.content,
            documentTitle: c.document?.title || '',
            similarity,
          };
        })
        .filter((r: any) => r.similarity > 0)
        .sort((a: any, b: any) => b.similarity - a.similarity)
        .slice(0, RAG_CONFIG.topK);

      this.logger.log(`[RagAgent] textSearch | matched=${scored.length} | topSim=${scored[0]?.similarity?.toFixed(3) || 'none'}`);

      return scored;
    } catch (err: any) {
      this.logger.warn(`[RagAgent] Text search failed: ${err.message}`);
      return [];
    }
  }

  private async generateEmbedding(text: string): Promise<number[] | null> {
    if (!this.openai) return null;
    try {
      const response = await this.retryWithBackoff(async () => {
        return await this.openai!.embeddings.create({
          model: RAG_CONFIG.embeddingModel,
          input: text.substring(0, 8000),
          dimensions: RAG_CONFIG.embeddingDimensions,
        });
      });
      return response.data[0]?.embedding || null;
    } catch (err: any) {
      this.logger.warn(`[RagAgent] Embedding generation failed: ${err.message}`);
      return null;
    }
  }

  private chunkText(text: string, chunkSize: number, overlap: number): string[] {
    const chunks: string[] = [];
    let start = 0;

    while (start < text.length) {
      const end = Math.min(start + chunkSize, text.length);
      let chunk = text.substring(start, end);

      if (end < text.length) {
        const lastSpace = chunk.lastIndexOf(' ');
        if (lastSpace > chunkSize * 0.5) {
          chunk = chunk.substring(0, lastSpace);
        }
      }

      const trimmed = chunk.trim();
      if (trimmed.length > 0) {
        chunks.push(trimmed);
      }

      const advance = Math.max(chunk.length - overlap, 1);
      start += advance;
    }

    return chunks;
  }

  private formatContext(chunks: RagChunkResult[]): string {
    const lines: string[] = ['--- CONTEXTO DA BASE DE CONHECIMENTO ---'];
    let totalChars = 0;

    for (const chunk of chunks) {
      let content = chunk.content;
      const title = `[${chunk.documentTitle}] `;
      const fullEntryLen = title.length + content.length;

      if (totalChars + fullEntryLen > RAG_CONFIG.maxContextChars) {
        const remaining = RAG_CONFIG.maxContextChars - totalChars - title.length;
        if (remaining > 150) {
          lines.push(title + content.substring(0, remaining) + '...');
        }
        break;
      }
      lines.push(title + content);
      totalChars += fullEntryLen;
    }

    lines.push('--- FIM DO CONTEXTO ---');
    return lines.join('\n');
  }
}
