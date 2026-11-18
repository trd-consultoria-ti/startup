export type AgentType = 'coordinator' | 'text' | 'vision' | 'audio' | 'document' | 'websearch' | 'scheduler' | 'rag' | 'intent' | 'summary';

export type AgentStatus = 'idle' | 'running' | 'completed' | 'failed';

export type SwarmTopology = 'hierarchical' | 'mesh' | 'pipeline' | 'parallel';

export type SwarmStrategy = 'specialized' | 'balanced' | 'adaptive';

export interface AgentConfig {
  type: AgentType;
  name: string;
  model?: string;
  capabilities?: string[];
  timeout?: number;
}

export interface AgentMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: number;
  metadata?: AgentMessageMetadata;
}

export interface AgentMessageMetadata {
  imageBase64?: string;
  imageMimeType?: string;
  imageCaption?: string;
  audioBuffer?: Buffer;
  documentBuffer?: Buffer;
  documentMimetype?: string;
  documentFileName?: string;
  webContext?: string;
  sourceAgent?: AgentType;
  targetAgent?: AgentType;
}

export interface AgentResult {
  response: string;
  sessionId?: string;
  status: AgentStatus;
  error?: string;
  metadata?: {
    model?: string;
    tokensUsed?: number;
    processingTime?: number;
    agentType?: AgentType;
    [key: string]: any;
  };
}

export interface AgentContext {
  instance: any;
  remoteJid: string;
  bot: any;
  session: any;
  settings: any;
  content: string;
  pushName?: string;
  msg?: any;
  conversationHistory: AgentMessage[];
  claudeTextSessionId?: string;
  claudeVisionSessionId?: string;
  webContext?: string;
  ragContext?: string;
}

export interface SwarmConfig {
  topology: SwarmTopology;
  maxAgents: number;
  strategy: SwarmStrategy;
  consensus?: 'raft' | 'byzantine' | 'gossip' | 'quorum';
  memory?: 'hybrid' | 'sqlite' | 'in-memory';
}

export interface SwarmAgentDef {
  type: AgentType;
  name: string;
  capabilities?: string[];
  role?: 'coordinator' | 'worker';
}

export const DEFAULT_SWARM_CONFIG: SwarmConfig = {
  topology: 'hierarchical',
  maxAgents: 8,
  strategy: 'specialized',
  consensus: 'raft',
  memory: 'hybrid',
};

export const DEFAULT_AGENT_TEAM: SwarmAgentDef[] = [
  { type: 'coordinator', name: 'Coordinator', role: 'coordinator' },
  { type: 'text', name: 'Text Agent', capabilities: ['text-processing', 'conversation'], role: 'worker' },
  { type: 'vision', name: 'Vision Agent', capabilities: ['image-analysis', 'ocr'], role: 'worker' },
  { type: 'audio', name: 'Audio Agent', capabilities: ['speech-to-text', 'transcription'], role: 'worker' },
  { type: 'document', name: 'Document Agent', capabilities: ['pdf-extraction', 'docx-extraction', 'txt-extraction'], role: 'worker' },
  { type: 'websearch', name: 'Web Search Agent', capabilities: ['web-search', 'duckduckgo'], role: 'worker' },
  { type: 'scheduler', name: 'Scheduler Agent', capabilities: ['reminders', 'scheduling', 'cron', 'natural-language-time'], role: 'worker' },
  { type: 'rag', name: 'RAG Agent', capabilities: ['knowledge-retrieval', 'semantic-search', 'document-qa'], role: 'worker' },
  { type: 'intent', name: 'Intent Classifier', capabilities: ['classification', 'routing'], role: 'worker' },
  { type: 'summary', name: 'Summary Agent', capabilities: ['summarization', 'context-compression'], role: 'worker' },
];

export const VISION_MODEL_CHAIN = (process.env.VISION_MODEL_CHAIN || 'gemma4:cloud,qwen3.5:cloud,llava:7b')
  .split(',')
  .map((m) => m.trim())
  .filter(Boolean);

export const TEXT_MODEL_CHAIN = (process.env.TEXT_MODEL_CHAIN || 'gpt-oss:120b-cloud,qwen3.5:cloud,gpt-oss:20b-cloud,qwen2.5-coder:14b,qwen3:8b,qwen2.5:3b')
  .split(',')
  .map((m) => m.trim())
  .filter(Boolean);

export const OLLAMA_CONFIG = {
  baseUrl: process.env.OLLAMA_BASE_URL || 'http://localhost:11434',
  apiKey: process.env.OLLAMA_API_KEY || '1a81279f0ff24b36baebdbc29ebdc66a',
  textModel: process.env.CLAUDE_CLI_MODEL || 'deepseek-v4-flash:0731-cloud',
  visionModel: process.env.CLAUDE_CLI_VISION_MODEL || 'gemma4:cloud',
  timeout: parseInt(process.env.CLAUDE_CLI_TIMEOUT || '120000', 10),
  whisperUrl: process.env.WHISPER_URL || 'http://localhost:9000',
};

export const SEARCH_CONFIG = {
  youtubeApiKey: process.env.YOUTUBE_API_KEY || '',
  googleSearchApiKey: process.env.GOOGLE_SEARCH_API_KEY || '',
  googleSearchCx: process.env.GOOGLE_SEARCH_CX || '',
  maxResults: parseInt(process.env.SEARCH_MAX_RESULTS || '8', 10),
  timeout: parseInt(process.env.SEARCH_TIMEOUT || '10000', 10),
};

export const SCHEDULER_CONFIG = {
  timezone: process.env.SCHEDULER_TIMEZONE || 'America/Sao_Paulo',
  maxRemindersPerUser: parseInt(process.env.SCHEDULER_MAX_REMINDERS || '50', 10),
  snoozeMinutes: parseInt(process.env.SCHEDULER_SNOOZE_MINUTES || '10', 10),
  maxSnoozeCount: parseInt(process.env.SCHEDULER_MAX_SNOOZE || '3', 10),
  pollIntervalMs: parseInt(process.env.SCHEDULER_POLL_INTERVAL || '60000', 10),
};

export interface AgentTrace {
  agentType: AgentType;
  inputLength: number;
  outputLength: number;
  latencyMs: number;
  model?: string;
  success: boolean;
  error?: string;
  timestamp: number;
}

export const RAG_CONFIG = {
  embeddingModel: process.env.RAG_EMBEDDING_MODEL || 'text-embedding-3-small',
  embeddingDimensions: parseInt(process.env.RAG_EMBEDDING_DIMS || '1536', 10),
  chunkSize: parseInt(process.env.RAG_CHUNK_SIZE || '1000', 10),
  chunkOverlap: parseInt(process.env.RAG_CHUNK_OVERLAP || '200', 10),
  topK: parseInt(process.env.RAG_TOP_K || '5', 10),
  minSimilarity: parseFloat(process.env.RAG_MIN_SIMILARITY || '0.7'),
  maxContextChars: parseInt(process.env.RAG_MAX_CONTEXT || '4000', 10),
};

export const CIRCUIT_BREAKER_CONFIG = {
  failureThreshold: parseInt(process.env.CIRCUIT_FAILURE_THRESHOLD || '3', 10),
  resetTimeoutMs: parseInt(process.env.CIRCUIT_RESET_TIMEOUT || '600000', 10),
  retryAttempts: parseInt(process.env.CIRCUIT_RETRY_ATTEMPTS || '2', 10),
  retryBackoffMs: parseInt(process.env.CIRCUIT_RETRY_BACKOFF || '1000', 10),
};

export const INTENT_CONFIG = {
  model: process.env.INTENT_MODEL || 'qwen2.5:3b',
  timeout: parseInt(process.env.INTENT_TIMEOUT || '5000', 10),
  enabled: process.env.INTENT_ENABLED !== 'false',
};
