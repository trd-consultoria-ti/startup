import { PrismaRepository } from '@api/repository/repository.service';
import { Logger } from '@config/logger.config';
import { RAG_CONFIG } from './agent-types';
import OpenAI from 'openai';

export interface MemoryEntry {
  key: string;
  value: string;
  remoteJid: string;
  botId: string;
  embedding?: number[];
  createdAt: number;
  updatedAt: number;
}

export class AgentMemory {
  private readonly logger = new Logger('AgentMemory');
  private readonly cache = new Map<string, MemoryEntry[]>();
  private readonly maxEntriesPerUser = 50;
  private openai: OpenAI | null = null;
  private redisCache: any = null;

  constructor(private readonly prismaRepository?: PrismaRepository) {
    const apiKey = process.env.OPENAI_API_KEY || process.env.RAG_OPENAI_API_KEY;
    if (apiKey) {
      this.openai = new OpenAI({ apiKey });
    }
  }

  setRedisCache(redisCache: any): void {
    this.redisCache = redisCache;
    this.logger.log('[Memory] Redis L1 cache enabled');
  }

  private hasPrisma(): boolean {
    return !!this.prismaRepository?.agentMemoryEntry;
  }

  private hasRedis(): boolean {
    return !!this.redisCache;
  }

  private redisKey(remoteJid: string, botId: string): string {
    return `agent_memory:${remoteJid}:${botId}`;
  }

  async store(remoteJid: string, botId: string, key: string, value: string): Promise<void> {
    const cacheKey = `${remoteJid}:${botId}`;
    let entries = this.cache.get(cacheKey) || [];

    let embedding: number[] | undefined;
    if (this.openai && !key.startsWith('pref:')) {
      embedding = await this.generateEmbedding(value);
    }

    const existing = entries.find((e) => e.key === key);
    if (existing) {
      existing.value = value;
      existing.updatedAt = Date.now();
      if (embedding) existing.embedding = embedding;
    } else {
      entries.push({ key, value, remoteJid, botId, embedding, createdAt: Date.now(), updatedAt: Date.now() });
      if (entries.length > this.maxEntriesPerUser) {
        entries = entries.slice(entries.length - this.maxEntriesPerUser);
      }
    }
    this.cache.set(cacheKey, entries);

    if (this.hasRedis()) {
      try {
        await this.redisCache.hSet(this.redisKey(remoteJid, botId), key, value);
      } catch (err: any) {
        this.logger.warn(`[Memory] Redis store failed for ${key}: ${err.message}`);
      }
    }

    if (this.hasPrisma()) {
      try {
        const embeddingStr = embedding ? `[${embedding.join(',')}]` : null;
        await this.prismaRepository.agentMemoryEntry.upsert({
          where: { remoteJid_botId_key: { remoteJid, botId, key } },
          create: { remoteJid, botId, key, value, embedding: embeddingStr },
          update: { value, embedding: embeddingStr },
        });
      } catch (err: any) {
        this.logger.warn(`[Memory] L2 store failed for ${key}: ${err.message}`);
      }
    }

    this.logger.log(`[Memory] Stored: ${key} for ${remoteJid}`);
  }

  async retrieve(remoteJid: string, botId: string, key?: string): Promise<MemoryEntry[]> {
    const cacheKey = `${remoteJid}:${botId}`;
    let entries = this.cache.get(cacheKey);

    if (!entries && this.hasRedis()) {
      try {
        const redisData = await this.redisCache.hGetAll?.(this.redisKey(remoteJid, botId));
        if (redisData && Object.keys(redisData).length > 0) {
          entries = Object.entries(redisData).map(([k, v]) => ({
            key: k,
            value: String(v),
            remoteJid,
            botId,
            createdAt: Date.now(),
            updatedAt: Date.now(),
          }));
          this.cache.set(cacheKey, entries);
        }
      } catch (err: any) {
        this.logger.warn(`[Memory] Redis retrieve failed: ${err.message}`);
      }
    }

    if (!entries && this.hasPrisma()) {
      try {
        const rows = await this.prismaRepository.agentMemoryEntry.findMany({
          where: { remoteJid, botId },
        });
        entries = rows.map((r: any) => ({
          key: r.key,
          value: r.value,
          remoteJid: r.remoteJid,
          botId: r.botId,
          embedding: r.embedding ? this.parseEmbedding(r.embedding) : undefined,
          createdAt: r.createdAt?.getTime() || Date.now(),
          updatedAt: r.updatedAt?.getTime() || Date.now(),
        }));
        this.cache.set(cacheKey, entries);

        if (this.hasRedis() && entries.length > 0) {
          for (const e of entries) {
            try {
              await this.redisCache.hSet(this.redisKey(remoteJid, botId), e.key, e.value);
            } catch {}
          }
        }
      } catch (err: any) {
        this.logger.warn(`[Memory] L2 retrieve failed: ${err.message}`);
        entries = [];
      }
    }

    entries = entries || [];
    if (key) {
      return entries.filter((e) => e.key === key);
    }
    return entries;
  }

  async search(remoteJid: string, botId: string, query: string): Promise<MemoryEntry[]> {
    const entries = await this.retrieve(remoteJid, botId);
    const lowerQuery = query.toLowerCase();
    return entries.filter(
      (e) => e.value.toLowerCase().includes(lowerQuery) || e.key.toLowerCase().includes(lowerQuery),
    );
  }

  async semanticSearch(remoteJid: string, botId: string, query: string, topK: number = 5): Promise<MemoryEntry[]> {
    const entries = await this.retrieve(remoteJid, botId);

    if (this.openai) {
      try {
        const queryEmbedding = await this.generateEmbedding(query);
        if (queryEmbedding) {
          const scored = entries
            .filter((e) => e.embedding)
            .map((e) => ({ entry: e, score: this.cosineSimilarity(queryEmbedding, e.embedding!) }))
            .filter((r) => r.score >= RAG_CONFIG.minSimilarity)
            .sort((a, b) => b.score - a.score)
            .slice(0, topK)
            .map((r) => r.entry);

          if (scored.length > 0) return scored;
        }
      } catch (err: any) {
        this.logger.warn(`[Memory] Semantic search failed: ${err.message} — falling back to text`);
      }
    }

    return this.search(remoteJid, botId, query);
  }

  private async generateEmbedding(text: string): Promise<number[] | undefined> {
    if (!this.openai) return undefined;
    try {
      const response = await this.openai.embeddings.create({
        model: RAG_CONFIG.embeddingModel,
        input: text.substring(0, 8000),
        dimensions: RAG_CONFIG.embeddingDimensions,
      });
      return response.data[0]?.embedding;
    } catch (err: any) {
      this.logger.warn(`[Memory] Embedding failed: ${err.message}`);
      return undefined;
    }
  }

  private cosineSimilarity(a: number[], b: number[]): number {
    let dot = 0, magA = 0, magB = 0;
    for (let i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      magA += a[i] * a[i];
      magB += b[i] * b[i];
    }
    const denom = Math.sqrt(magA) * Math.sqrt(magB);
    return denom > 0 ? dot / denom : 0;
  }

  private parseEmbedding(str: string): number[] | undefined {
    try {
      const cleaned = str.replace(/^\[|\]$/g, '');
      return cleaned.split(',').map((n) => parseFloat(n.trim()));
    } catch {
      return undefined;
    }
  }

  async storeConversationSummary(remoteJid: string, botId: string, summary: string): Promise<void> {
    await this.store(remoteJid, botId, 'conversation_summary', summary);
  }

  async getConversationSummary(remoteJid: string, botId: string): Promise<string | null> {
    const entries = await this.retrieve(remoteJid, botId, 'conversation_summary');
    return entries.length > 0 ? entries[0].value : null;
  }

  async storeUserPreference(remoteJid: string, botId: string, prefKey: string, prefValue: string): Promise<void> {
    await this.store(remoteJid, botId, `pref:${prefKey}`, prefValue);
  }

  async getUserPreferences(remoteJid: string, botId: string): Promise<Record<string, string>> {
    const entries = await this.retrieve(remoteJid, botId);
    const prefs: Record<string, string> = {};
    for (const e of entries) {
      if (e.key.startsWith('pref:')) {
        prefs[e.key.substring(5)] = e.value;
      }
    }
    return prefs;
  }

  buildContextFromMemory(remoteJid: string, botId: string, entries: MemoryEntry[]): string {
    if (entries.length === 0) return '';
    const parts: string[] = [];
    const summary = entries.find((e) => e.key === 'conversation_summary');
    if (summary) {
      parts.push(`[Resumo de conversas anteriores: ${summary.value}]`);
    }
    const prefs = entries.filter((e) => e.key.startsWith('pref:'));
    if (prefs.length > 0) {
      const prefStr = prefs.map((p) => `${p.key.substring(5)}: ${p.value}`).join(', ');
      parts.push(`[Preferências do usuário: ${prefStr}]`);
    }
    return parts.join('\n');
  }

  async clear(remoteJid: string, botId: string): Promise<void> {
    const cacheKey = `${remoteJid}:${botId}`;
    this.cache.delete(cacheKey);

    if (this.hasRedis()) {
      try {
        await this.redisCache.delete?.(this.redisKey(remoteJid, botId));
      } catch (err: any) {
        this.logger.warn(`[Memory] Redis clear failed: ${err.message}`);
      }
    }

    if (this.hasPrisma()) {
      try {
        await this.prismaRepository.agentMemoryEntry.deleteMany({
          where: { remoteJid, botId },
        });
      } catch (err: any) {
        this.logger.warn(`[Memory] L2 clear failed: ${err.message}`);
      }
    }

    this.logger.log(`[Memory] Cleared for ${remoteJid}`);
  }
}
