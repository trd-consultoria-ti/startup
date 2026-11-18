-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to AgentMemoryEntry for semantic search
ALTER TABLE "AgentMemoryEntry" ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- Create RagDocument table
CREATE TABLE IF NOT EXISTS "RagDocument" (
    id TEXT NOT NULL,
    botId TEXT NOT NULL,
    title TEXT NOT NULL,
    source TEXT,
    content TEXT NOT NULL,
    createdAt TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "RagDocument_pkey" PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS "RagDocument_botId_idx" ON "RagDocument"("botId");

-- Create RagChunk table
CREATE TABLE IF NOT EXISTS "RagChunk" (
    id TEXT NOT NULL,
    documentId TEXT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    index INTEGER NOT NULL,
    CONSTRAINT "RagChunk_pkey" PRIMARY KEY (id),
    CONSTRAINT "RagChunk_documentId_fkey" FOREIGN KEY ("documentId") REFERENCES "RagDocument"("id") ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS "RagChunk_documentId_idx" ON "RagChunk"("documentId");

-- Create AgentTrace table
CREATE TABLE IF NOT EXISTS "AgentTrace" (
    id TEXT NOT NULL,
    remoteJid TEXT,
    botId TEXT,
    agentType TEXT NOT NULL,
    inputLength INTEGER NOT NULL DEFAULT 0,
    outputLength INTEGER NOT NULL DEFAULT 0,
    latencyMs INTEGER NOT NULL DEFAULT 0,
    model TEXT,
    success BOOLEAN NOT NULL DEFAULT true,
    error TEXT,
    topology TEXT,
    strategy TEXT,
    agentsInvolved TEXT,
    createdAt TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "AgentTrace_pkey" PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS "AgentTrace_remoteJid_botId_idx" ON "AgentTrace"("remoteJid", "botId");
CREATE INDEX IF NOT EXISTS "AgentTrace_agentType_idx" ON "AgentTrace"("agentType");
CREATE INDEX IF NOT EXISTS "AgentTrace_createdAt_idx" ON "AgentTrace"("createdAt");
